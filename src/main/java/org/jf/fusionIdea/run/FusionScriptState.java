/*
 * Copyright 2020, Ben Gruver
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation and/or
 * other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors
 * may be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.jf.fusionIdea.run;

import com.google.common.collect.ImmutableMap;
import com.google.common.io.BaseEncoding;
import com.google.common.io.ByteStreams;
import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.gson.Gson;
import com.intellij.execution.DefaultExecutionResult;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionResult;
import com.intellij.execution.Executor;
import com.intellij.execution.impl.ConsoleViewImpl;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.process.ProcessOutputTypes;
import com.intellij.execution.runners.DebuggableRunProfileState;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.concurrency.Promise;
import org.jetbrains.concurrency.Promises;
import org.jetbrains.ide.PooledThreadExecutor;
import org.jf.fusionIdea.FusionIdeaPlugin;
import org.jf.fusionIdea.facet.FusionFacet;
import rawhttp.core.RawHttp;
import rawhttp.core.RawHttpResponse;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.util.Collections;
import java.util.Enumeration;

public class FusionScriptState implements DebuggableRunProfileState {
    private final Project project;
    @Nullable private final FusionRunConfiguration fusionRunConfiguration;
    private final int pid;
    private final boolean debug;

    private ServerSocket serverSocket;

    public FusionScriptState(Project project, @Nullable FusionRunConfiguration fusionRunConfiguration,
                             int pid, boolean debug) {
        this.project = project;
        this.fusionRunConfiguration = fusionRunConfiguration;
        this.pid = pid;
        this.debug = debug;
    }

    public int getPid() {
        return pid;
    }

    public ServerSocket getServerSocket() throws ExecutionException {
        if (serverSocket == null) {
            try {
                serverSocket = new ServerSocket(0);
            } catch (IOException ex) {
                throw new ExecutionException("Failed to find free socket port", ex);
            }
        }
        return serverSocket;
    }

    @Nullable @Override
    public ExecutionResult execute(Executor executor, @NotNull ProgramRunner runner) {
        throw new UnsupportedOperationException();
    }

    @NotNull @Override public Promise<ExecutionResult> execute(int debugPort) {
        ConsoleViewImpl consoleView = new ConsoleViewImpl(project, false);
        FusionDebugProcessHandler processHandler = new FusionDebugProcessHandler(project);
        consoleView.attachToProcess(processHandler);
        processHandler.notifyTextAvailable("Public key hash: " + getPublicKeyHash() + "\n",
                ProcessOutputTypes.SYSTEM);

        connectToFusionAndStartScript(processHandler)
                .addCallback(new FutureCallback<Void>() {
            @Override public void onSuccess(Void unused) {
            }

            @Override public void onFailure(@NotNull Throwable throwable) {
                processHandler.notifyTextAvailable(
                        "Encountered error while attempting to connect to Fusion.\n", ProcessOutputTypes.SYSTEM);
                FusionIdeaPlugin.log.error("Encountered error while attempting to connect to Fusion", throwable);
                processHandler.destroyProcess();
            }
        }, command -> ApplicationManager.getApplication().invokeLater(command));

        return Promises.resolvedPromise(new DefaultExecutionResult(consoleView, processHandler));
    }

    private FluentFuture<Void> connectToFusionAndStartScript(ProcessHandler processHandler) {
        ListeningExecutorService executor = MoreExecutors.listeningDecorator(PooledThreadExecutor.INSTANCE);
        FluentFuture<Integer> portFuture = new SSDPServer(pid).start(executor, processHandler);
        return portFuture.transform(port -> {
            assert port != null;
            sendStartScriptHttpRequest(port);
            return null;
        }, executor);
    }

    private void sendStartScriptHttpRequest(int port) {
        IdeaPluginDescriptor plugin = PluginManagerCore.getPlugin(PluginId.getId(FusionIdeaPlugin.ID));
        assert plugin != null;

        String pydevdPath = new File(new File(plugin.getPath(), "lib"), "pydevd-1.9.0").getAbsolutePath();

        ImmutableMap.Builder<String, String> innerRequestBuilder = ImmutableMap.builder();

        if (fusionRunConfiguration != null) {
            innerRequestBuilder.put("script", new File(fusionRunConfiguration.getScript()).getAbsolutePath());
        }

        innerRequestBuilder.put("debug", debug ? "1" : "0");
        innerRequestBuilder.put("pydevd_path", pydevdPath);
        innerRequestBuilder.put("nonce", Long.toString(getNextNonce()));
        try {
            innerRequestBuilder.put("debug_port", Integer.toString(getServerSocket().getLocalPort()));
        } catch (ExecutionException ex) {
            throw new RuntimeException(ex);
        }

        Gson gson = new Gson();
        ImmutableMap.Builder<String, String> outerRequestBuilder = ImmutableMap.builder();
        try {
            String innerRequest = gson.toJson(innerRequestBuilder.build());
            KeyPair keyPair = getKeyPair();
            byte[] signature = sign(keyPair.getPrivate(), innerRequest);

            outerRequestBuilder.put("message", innerRequest);
            outerRequestBuilder.put("signature", BaseEncoding.base16().encode(signature));
            outerRequestBuilder.put("pubkey_modulus", ((RSAPublicKey)keyPair.getPublic()).getModulus().toString());
            outerRequestBuilder.put("pubkey_exponent",
                    ((RSAPublicKey)keyPair.getPublic()).getPublicExponent().toString());
        } catch (NoSuchAlgorithmException | SignatureException | InvalidKeyException ex) {
            throw new RuntimeException(ex);
        }

        try {
            URL url = new URL("http", "127.0.0.1", port, "");
            HttpURLConnection con = (HttpURLConnection)url.openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/json; utf-8");
            con.setDoOutput(true);
            byte[] input = gson.toJson(outerRequestBuilder.build()).getBytes(StandardCharsets.UTF_8);
            con.setFixedLengthStreamingMode(input.length);

            try (OutputStream os = con.getOutputStream()) {
                os.write(input, 0, input.length);
            }

            if (con.getResponseCode() != 200) {
                throw new IOException("Invalid response");
            }
            try (InputStream is = con.getInputStream()) {
                ByteStreams.toByteArray(is);
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private class SSDPServer {
        private final int targetPid;

        public SSDPServer(int targetPid) {
            this.targetPid = targetPid;
        }

        private byte[] SEARCH_MESSAGE =
                ("M-SEARCH * HTTP/1.1\r\n" +
                "MAN: \"ssdp:discover\"\r\n" +
                "MX: 1\r\n" +
                "ST: fusion_idea:debug\r\n" +
                "HOST: 127.0.0.1:1900\r\n\r\n").getBytes(StandardCharsets.UTF_8);

        private MulticastSocket sendIpv4SSDPRequest() {
            try {
                InetAddress localhost = InetAddress.getByName("127.0.0.1");
                InetSocketAddress multicastAddress = new InetSocketAddress(
                        InetAddress.getByName("239.172.243.75"), 1900);
                MulticastSocket socket = new MulticastSocket(new InetSocketAddress(localhost, 0));
                socket.setLoopbackMode(/* disabled= */ false);
                socket.send(new DatagramPacket(SEARCH_MESSAGE, SEARCH_MESSAGE.length, multicastAddress));

                return socket;
            } catch (IOException ex) {
                FusionIdeaPlugin.log.debug("ipv4 ssdp failed");
                return null;
            }
        }

        private MulticastSocket sendIpv6SSDPRequest() {
            try {
                Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();

                InetSocketAddress multicastAddress = new InetSocketAddress(
                        "ff01:fb68:e6b7:45f9:4acc:2559:6c6e:c014", 1900);
                MulticastSocket socket = new MulticastSocket(0);
                socket.setLoopbackMode(/* disabled= */ false);

                boolean success = false;
                for (NetworkInterface netint : Collections.list(nets)) {
                    try {
                        if (netint.supportsMulticast()) {
                            boolean hasIpV6 = false;
                            for (InetAddress address : Collections.list(netint.getInetAddresses())) {
                                if (address instanceof Inet6Address) {
                                    hasIpV6 = true;
                                    break;
                                }
                            }
                            if (hasIpV6) {
                                socket.setNetworkInterface(netint);
                                socket.send(new DatagramPacket(SEARCH_MESSAGE, SEARCH_MESSAGE.length, multicastAddress));
                                success = true;
                            }
                        }
                    } catch (IOException ex) {
                        FusionIdeaPlugin.log.debug("ipv6 multicast failed on " + netint.getName(), ex);
                    }
                }

                if (!success) {
                    FusionIdeaPlugin.log.error("Couldn't send ipv6 ssdp packet on any interface");
                    return null;
                }

                return socket;
            } catch (IOException ex) {
                FusionIdeaPlugin.log.debug("ipv6 ssdp failed", ex);
                return null;
            }
        }

        public FluentFuture<Integer> start(ListeningExecutorService executor, ProcessHandler processHandler) {
            return FluentFuture.from(executor.submit(() -> {
                MulticastSocket socket;

                socket = sendIpv6SSDPRequest();

                if (socket == null) {
                    socket = sendIpv4SSDPRequest();
                }

                if (socket == null) {
                    throw new RuntimeException("Couldn't send SSDP request via ipv6 or ipv4");
                }

                socket.setSoTimeout(1000);

                RawHttp rawHttp = new RawHttp();
                long startTime = System.nanoTime();
                DatagramPacket receivedPacket = new DatagramPacket(new byte[1024], 1024);
                int remoteDebugPort = -1;
                do {
                    try {
                        socket.receive(receivedPacket);
                    } catch (SocketTimeoutException ex) {
                        break;
                    }

                    String data = new String(receivedPacket.getData(), 0, receivedPacket.getLength());
                    RawHttpResponse<Void> response = rawHttp.parseResponse(data);
                    if (response.getStatusCode() != 200) {
                        FusionIdeaPlugin.log.debug(
                                "Got SSDP response with unexpected status: %d", response.getStatusCode());
                        continue;
                    }

                    String stHeader = response.getHeaders().getFirst("ST").orElse(null);
                    if (!"fusion_idea:debug".equals(stHeader)) {
                        if (stHeader == null) {
                            FusionIdeaPlugin.log.debug("Response missing ST header");
                        } else {
                            FusionIdeaPlugin.log.debug("Got unexpected ST header: %s", stHeader);
                        }
                        continue;
                    }

                    String usnHeader = response.getHeaders().getFirst("USN").orElse(null);
                    if (usnHeader == null) {
                        FusionIdeaPlugin.log.debug("Response missing USN header");
                        continue;
                    }
                    if (!usnHeader.startsWith("pid:")) {
                        FusionIdeaPlugin.log.debug("Unexpected format for USN header: %s", usnHeader);
                    }

                    int pid;
                    try {
                        pid = Integer.parseInt(usnHeader.split(":")[1]);
                    } catch (NumberFormatException ex) {
                        FusionIdeaPlugin.log.debug("Unexpected format for USN header: %s", usnHeader);
                        continue;
                    }

                    if (pid != targetPid) {
                        FusionIdeaPlugin.log.debug("Got valid pid %d, which isn't the pid we're looking for.", pid);
                        continue;
                    }

                    String locationHeader = response.getHeaders().getFirst("Location").orElse(null);
                    if (locationHeader == null) {
                        FusionIdeaPlugin.log.debug("Response missing Location header.");
                        continue;
                    }
                    if (!locationHeader.startsWith("127.0.0.1:")) {
                        FusionIdeaPlugin.log.debug(
                                "Got remote location (%s), but expecting localhost.", locationHeader);
                        continue;
                    }
                    int remotePort;
                    try {
                        remotePort = Integer.parseInt(locationHeader.split(":")[1]);
                    } catch (NumberFormatException ex) {
                        FusionIdeaPlugin.log.debug("Unexpected format for Location header: %s", locationHeader);
                        continue;
                    }

                    String serverHeader = response.getHeaders().getFirst("SERVER").orElse(null);
                    if (!serverHeader.startsWith("fusion_idea/")) {
                        FusionIdeaPlugin.log.debug("Unexpected format for SERVER header: %s", serverHeader);
                    } else {
                        String versionString = serverHeader.substring(12);
                        try {
                            float version = Float.parseFloat(versionString);

                            FusionFacet fusionFacet;

                            if (fusionRunConfiguration != null) {
                                fusionFacet = FusionFacet.getInstance(fusionRunConfiguration.getModule());
                            } else {
                                fusionFacet = FusionFacet.getInstance(project);
                            }
                            Float latestVersion = fusionFacet.getLatestAddinVersion();

                            if (latestVersion != null && latestVersion > version) {
                                processHandler.notifyTextAvailable(
                                        "\nA new version of fusion_idea_addin is available: " + latestVersion + "\n" +
                                            "See https://github.com/JesusFreke/fusion_idea_addin/wiki/Installing-the-" +
                                            "add-in-in-Fusion-360 for installation instructions.\n\n",
                                        ProcessOutputTypes.SYSTEM);
                            }
                        } catch (NumberFormatException ex) {
                            FusionIdeaPlugin.log.debug("Unexpected format for version number: %f", versionString);
                        }
                    }
                    remoteDebugPort = remotePort;
                    break;
                } while (Duration.ofNanos(System.nanoTime() - startTime).compareTo(Duration.ofSeconds(1)) <= 0);

                if (remoteDebugPort <= 0) {
                    processHandler.notifyTextAvailable(String.format(
                            "Could not contact Fusion 360 process %d. Is the add-in running?\n" +
                                    "See https://github.com/JesusFreke/fusion_idea_addin/wiki/Installing-the-add-in-" +
                                    "in-Fusion-360 for more details.\n", targetPid),
                            ProcessOutputTypes.SYSTEM);
                    throw new RuntimeException("Did not receive a debug port for pid " + targetPid);
                }

                return remoteDebugPort;
            }));
        }
    }

    private static KeyPair keyPair;
    private static KeyPair getKeyPair() {
        try {
            if (keyPair == null) {
                KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
                keyGen.initialize(2048);

                keyPair = keyGen.generateKeyPair();
            }
            return keyPair;
        } catch (NoSuchAlgorithmException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static long nonce = 0;
    private static long getNextNonce() {
        return ++nonce;
    }

    private static byte[] sign(PrivateKey privateKey, String message)
            throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        Signature signature = Signature.getInstance("SHA1WithRSA");
        signature.initSign(privateKey);
        signature.update(message.getBytes(StandardCharsets.UTF_8));
        return signature.sign();
    }

    private static String publicKeyHash;
    private static String getPublicKeyHash() {
        if (publicKeyHash == null) {
            try {
                RSAPublicKey publicKey = (RSAPublicKey)getKeyPair().getPublic();

                MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
                sha1.reset();
                sha1.update((publicKey.getModulus().toString() + ":" + publicKey.getPublicExponent().toString()).getBytes());

                publicKeyHash = BaseEncoding.base16().encode(sha1.digest());
            } catch (NoSuchAlgorithmException ex) {
                throw new RuntimeException(ex);
            }
        }
        return publicKeyHash;
    }
}
