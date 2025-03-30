package org.printed.chat;

import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Player {

    private int vaoId;
    private int vboId;
    private int texCoordsVboId;
    private int textureId;
    private int shaderProgramId;

    private float x = 0.0f;
    private float y = 0.0f;
    private float speed = 0.01f;
    private float width = 0.2f;
    private float height = 0.3f;

    public Player() {
        init();
    }

    private void init() {
        // Crear el shader program para texturas
        createShaderProgram();

        // Crear el VAO
        vaoId = GL30.glGenVertexArrays();
        GL30.glBindVertexArray(vaoId);

        // Crear los vértices del jugador (un rectángulo simple que representará al personaje)
        try (MemoryStack stack = MemoryStack.stackPush()) {
            // Vértices del rectángulo
            float[] vertices = new float[]{
                    -width/2, height/2, 0.0f,  // Esquina superior izquierda
                    width/2, height/2, 0.0f,   // Esquina superior derecha
                    width/2, -height/2, 0.0f,  // Esquina inferior derecha
                    -width/2, -height/2, 0.0f  // Esquina inferior izquierda
            };

            // Coordenadas de textura
            float[] texCoords = new float[]{
                    0.0f, 0.0f,  // Esquina superior izquierda
                    1.0f, 0.0f,  // Esquina superior derecha
                    1.0f, 1.0f,  // Esquina inferior derecha
                    0.0f, 1.0f   // Esquina inferior izquierda
            };

            // Crear y llenar el VBO de vértices
            FloatBuffer vertexBuffer = stack.mallocFloat(vertices.length);
            vertexBuffer.put(vertices).flip();

            vboId = GL15.glGenBuffers();
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboId);
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, vertexBuffer, GL15.GL_STATIC_DRAW);
            GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 0, 0);
            GL20.glEnableVertexAttribArray(0);

            // Crear y llenar el VBO de coordenadas de textura
            FloatBuffer texCoordsBuffer = stack.mallocFloat(texCoords.length);
            texCoordsBuffer.put(texCoords).flip();

            texCoordsVboId = GL15.glGenBuffers();
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, texCoordsVboId);
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, texCoordsBuffer, GL15.GL_STATIC_DRAW);
            GL20.glVertexAttribPointer(1, 2, GL11.GL_FLOAT, false, 0, 0);
            GL20.glEnableVertexAttribArray(1);
        }

        // Cargar textura
        loadTexture("robot.png");

        // Desenlazar el VAO
        GL30.glBindVertexArray(0);
    }

    private void createShaderProgram() {
        // Vertex Shader con soporte para texturas
        int vertexShaderId = GL20.glCreateShader(GL20.GL_VERTEX_SHADER);
        GL20.glShaderSource(vertexShaderId,
                "#version 410 core\n" +
                        "layout (location = 0) in vec3 position;\n" +
                        "layout (location = 1) in vec2 texCoord;\n" +
                        "out vec2 TexCoord;\n" +
                        "uniform vec2 offset;\n" +
                        "void main() {\n" +
                        "    gl_Position = vec4(position.x + offset.x, position.y + offset.y, position.z, 1.0);\n" +
                        "    TexCoord = texCoord;\n" +
                        "}");
        GL20.glCompileShader(vertexShaderId);

        // Verificar errores de compilación
        if (GL20.glGetShaderi(vertexShaderId, GL20.GL_COMPILE_STATUS) == 0) {
            System.err.println("Error al compilar el Vertex Shader: " + GL20.glGetShaderInfoLog(vertexShaderId));
            return;
        }

        // Fragment Shader con soporte para texturas
        int fragmentShaderId = GL20.glCreateShader(GL20.GL_FRAGMENT_SHADER);
        GL20.glShaderSource(fragmentShaderId,
                "#version 410 core\n" +
                        "in vec2 TexCoord;\n" +
                        "out vec4 fragColor;\n" +
                        "uniform sampler2D textureSampler;\n" +
                        "void main() {\n" +
                        "    fragColor = texture(textureSampler, TexCoord);\n" +
                        "}");
        GL20.glCompileShader(fragmentShaderId);

        // Verificar errores de compilación
        if (GL20.glGetShaderi(fragmentShaderId, GL20.GL_COMPILE_STATUS) == 0) {
            System.err.println("Error al compilar el Fragment Shader: " + GL20.glGetShaderInfoLog(fragmentShaderId));
            return;
        }

        // Crear y enlazar el Shader Program
        shaderProgramId = GL20.glCreateProgram();
        GL20.glAttachShader(shaderProgramId, vertexShaderId);
        GL20.glAttachShader(shaderProgramId, fragmentShaderId);
        GL20.glLinkProgram(shaderProgramId);

        // Verificar errores de enlace
        if (GL20.glGetProgrami(shaderProgramId, GL20.GL_LINK_STATUS) == 0) {
            System.err.println("Error al enlazar el Shader Program: " + GL20.glGetProgramInfoLog(shaderProgramId));
            return;
        }

        // Después de enlazar, podemos eliminar los shaders
        GL20.glDeleteShader(vertexShaderId);
        GL20.glDeleteShader(fragmentShaderId);
    }

    private void loadTexture(String fileName) {
        textureId = TextureLoader.loadTexture(fileName);
        if (textureId == -1) {
            throw new RuntimeException("Error al cargar la textura del jugador: " + fileName);
        }
    }

    public void render() {
        // Usar el shader program
        GL20.glUseProgram(shaderProgramId);

        // Actualizar la posición del jugador en el shader
        int offsetLocation = GL20.glGetUniformLocation(shaderProgramId, "offset");
        GL20.glUniform2f(offsetLocation, x, y);

        // Activar la unidad de textura 0
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);

        // Configurar el sampler para usar la unidad de textura 0
        int textureSamplerLocation = GL20.glGetUniformLocation(shaderProgramId, "textureSampler");
        GL20.glUniform1i(textureSamplerLocation, 0);

        // Enlazar el VAO
        GL30.glBindVertexArray(vaoId);

        // Habilitar transparencias
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        // Dibujar el rectángulo como 2 triángulos (un quad)
        GL11.glDrawArrays(GL11.GL_TRIANGLE_FAN, 0, 4);

        // Deshabilitar transparencias
        GL11.glDisable(GL11.GL_BLEND);

        // Desenlazar el VAO
        GL30.glBindVertexArray(0);

        // Dejar de usar el shader program
        GL20.glUseProgram(0);
    }

    public void handleInput(long window, int key, int action) {
        if (action == GLFW.GLFW_PRESS || action == GLFW.GLFW_REPEAT) {
            switch (key) {
                case GLFW.GLFW_KEY_W:
                    y += speed;
                    break;
                case GLFW.GLFW_KEY_S:
                    y -= speed;
                    break;
                case GLFW.GLFW_KEY_A:
                    x -= speed;
                    break;
                case GLFW.GLFW_KEY_D:
                    x += speed;
                    break;
            }
        }
    }

    public void cleanup() {
        // Eliminar los VBOs
        GL15.glDeleteBuffers(vboId);
        GL15.glDeleteBuffers(texCoordsVboId);

        // Eliminar el VAO
        GL30.glDeleteVertexArrays(vaoId);

        // Eliminar la textura
        GL11.glDeleteTextures(textureId);

        // Eliminar el shader program
        GL20.glDeleteProgram(shaderProgramId);
    }
}