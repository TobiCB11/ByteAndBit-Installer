package de.byteandbit;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.byteandbit.data.GameInstance;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.instrument.Instrumentation;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class Agent {
    /**
     * EntryPoint of a dynamically injected agent.
     *
     * @param agentArgs command line arguments from injecting the agent.
     * @param inst      reference to the instrumentation API.
     */
    public static void agentmain(String agentArgs, Instrumentation inst) {
        String[] args = agentArgs.split(" ");
        int pid = Integer.parseInt(args[0]);
        int port = Integer.parseInt(args[1]);
        if (isMinecraft()) {
            GameInstance instance = new GameInstance(pid, gameDir(), mcVersion(), isForge());
            post(port, instance);
        }
    }

    public static String mcVersion() {
        try (java.io.InputStream is = Agent.class.getResourceAsStream("/version.json")) {
            if (is != null) {
                com.fasterxml.jackson.databind.JsonNode node = new ObjectMapper().readTree(is);
                if (node.has("id")) {
                    return node.get("id").asText().replaceAll("-.*", "");
                }
            }
        } catch (Exception ignored) {
        }
        return "unknown";
    }

    public static boolean isForge() {
        for (String key : System.getProperties().stringPropertyNames()) {
            if (key.startsWith("fml.") || key.startsWith("forge.")) {
                return true;
            }
        }
        try {
            Class.forName("net.minecraftforge.fml.common.Loader", false,
                    ClassLoader.getSystemClassLoader());
            return true;
        } catch (ClassNotFoundException ignored) {
        }
        return false;
    }

    public static boolean isMinecraft() {
        if (System.getProperty("minecraft.launcher.brand") != null) {
            return true;
        }
        ClassLoader cl = ClassLoader.getSystemClassLoader();
        if (cl.getResource("assets/minecraft/lang/en_us.lang") != null ||
                cl.getResource("assets/minecraft/lang/en_us.json") != null) {
            return true;
        }

        return false;
    }

    public static String gameDir() {
        return System.getProperty("user.dir");
    }

    public static void post(int port, GameInstance gameInstance) {
        try {
            String json = new ObjectMapper().writeValueAsString(gameInstance);
            try (Socket socket = new Socket("localhost", port);
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
                out.print(json);
                out.flush();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}