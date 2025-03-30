package org.printed.chat;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryUtil;

public class Game {

    private long window;
    private final int WIDTH = 800;
    private final int HEIGHT = 600;
    private final String TITLE = "Mi Juego 2D";

    private Player player;

    public Game() {
        init();
    }

    private void init() {
        // Configurar el callback de error
        GLFWErrorCallback.createPrint(System.err).set();

        // Inicializar GLFW
        if (!GLFW.glfwInit()) {
            throw new IllegalStateException("No se pudo inicializar GLFW");
        }

        // Configurar la ventana GLFW
        GLFW.glfwDefaultWindowHints();
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_TRUE);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 4);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 1);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_FORWARD_COMPAT, GLFW.GLFW_TRUE);

        // Crear la ventana
        window = GLFW.glfwCreateWindow(WIDTH, HEIGHT, TITLE, MemoryUtil.NULL, MemoryUtil.NULL);
        if (window == MemoryUtil.NULL) {
            throw new RuntimeException("No se pudo crear la ventana GLFW");
        }

        // Configurar callbacks de teclado
        GLFW.glfwSetKeyCallback(window, (window, key, scancode, action, mods) -> {
            if (key == GLFW.GLFW_KEY_ESCAPE && action == GLFW.GLFW_RELEASE) {
                GLFW.glfwSetWindowShouldClose(window, true);
            }

            // Manejar los controles del jugador
            if (player != null) {
                player.handleInput(window, key, action);
            }
        });

        // Obtener la resolución del monitor primario
        var vidmode = GLFW.glfwGetVideoMode(GLFW.glfwGetPrimaryMonitor());

        // Centrar la ventana
        GLFW.glfwSetWindowPos(
                window,
                (vidmode.width() - WIDTH) / 2,
                (vidmode.height() - HEIGHT) / 2
        );

        // Hacer que el contexto OpenGL sea actual
        GLFW.glfwMakeContextCurrent(window);

        // Activar v-sync
        GLFW.glfwSwapInterval(1);

        // Mostrar la ventana
        GLFW.glfwShowWindow(window);

        // Inicializar OpenGL
        GL.createCapabilities();

        // Establecer el color de fondo
        GL11.glClearColor(0.2f, 0.3f, 0.3f, 1.0f);

        // Inicializar el jugador
        player = new Player();
    }

    public void start() {
        gameLoop();
        cleanup();
    }

    private void gameLoop() {
        while (!GLFW.glfwWindowShouldClose(window)) {
            // Manejar eventos
            GLFW.glfwPollEvents();

            // Renderizar
            render();

            // Actualizar la ventana
            GLFW.glfwSwapBuffers(window);
        }
    }

    private void render() {
        // Limpiar la pantalla
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);

        // Dibujar el jugador
        player.render();
    }

    private void cleanup() {
        // Liberar el jugador
        player.cleanup();

        // Liberar las texturas (si usas la clase TextureLoader)
        TextureLoader.cleanup();

        // Destruir la ventana
        GLFW.glfwDestroyWindow(window);

        // Terminar GLFW
        GLFW.glfwTerminate();
        GLFW.glfwSetErrorCallback(null).free();
    }
}