package org.printed.chat;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryStack;

import java.nio.FloatBuffer;

public class Background {
    private int vaoId;
    private int vboId;
    private int texCoordsVboId;
    private int textureId;
    private int shaderProgramId;

    private float scrollSpeed = 0.0f; // 0 para fondo estático, > 0 para desplazamiento
    private float offsetX = 0.0f;
    private float offsetY = 0.0f;

    public Background(String textureFileName) {
        init(textureFileName);
    }

    private void init(String textureFileName) {
        // Crear el shader program para el fondo
        createShaderProgram();

        // Crear el VAO
        vaoId = GL30.glGenVertexArrays();
        GL30.glBindVertexArray(vaoId);

        // Crear los vértices (un rectángulo que cubre toda la pantalla)
        try (MemoryStack stack = MemoryStack.stackPush()) {
            // Vértices para pantalla completa (-1 a 1 en ambos ejes)
            float[] vertices = new float[]{
                    -1.0f, 1.0f, 0.0f,  // Esquina superior izquierda
                    1.0f, 1.0f, 0.0f,   // Esquina superior derecha
                    1.0f, -1.0f, 0.0f,  // Esquina inferior derecha
                    -1.0f, -1.0f, 0.0f  // Esquina inferior izquierda
            };

            // Coordenadas de textura (puedes ajustar estos valores para repetir la textura)
            float[] texCoords = new float[]{
                    0.0f, 1.0f,  // Esquina superior izquierda
                    1.0f, 1.0f,  // Esquina superior derecha
                    1.0f, 0.0f,  // Esquina inferior derecha
                    0.0f, 0.0f   // Esquina inferior izquierda
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

        // Cargar textura del fondo
        // Puedes usar la clase TextureLoader si la implementaste
        textureId = TextureLoader.loadTexture(textureFileName);

        // Desenlazar el VAO
        GL30.glBindVertexArray(0);
    }

    private void createShaderProgram() {
        // Vertex Shader con soporte para desplazamiento de textura
        int vertexShaderId = GL20.glCreateShader(GL20.GL_VERTEX_SHADER);
        GL20.glShaderSource(vertexShaderId,
                "#version 410 core\n" +
                        "layout (location = 0) in vec3 position;\n" +
                        "layout (location = 1) in vec2 texCoord;\n" +
                        "out vec2 TexCoord;\n" +
                        "uniform vec2 texOffset;\n" +
                        "void main() {\n" +
                        "    gl_Position = vec4(position, 1.0);\n" +
                        "    TexCoord = texCoord + texOffset;\n" +
                        "}");
        GL20.glCompileShader(vertexShaderId);

        // Verificar errores de compilación
        if (GL20.glGetShaderi(vertexShaderId, GL20.GL_COMPILE_STATUS) == 0) {
            System.err.println("Error al compilar el Vertex Shader de fondo: " +
                    GL20.glGetShaderInfoLog(vertexShaderId));
            return;
        }

        // Fragment Shader
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
            System.err.println("Error al compilar el Fragment Shader de fondo: " +
                    GL20.glGetShaderInfoLog(fragmentShaderId));
            return;
        }

        // Crear y enlazar el Shader Program
        shaderProgramId = GL20.glCreateProgram();
        GL20.glAttachShader(shaderProgramId, vertexShaderId);
        GL20.glAttachShader(shaderProgramId, fragmentShaderId);
        GL20.glLinkProgram(shaderProgramId);

        // Verificar errores de enlace
        if (GL20.glGetProgrami(shaderProgramId, GL20.GL_LINK_STATUS) == 0) {
            System.err.println("Error al enlazar el Shader Program de fondo: " +
                    GL20.glGetProgramInfoLog(shaderProgramId));
            return;
        }

        // Después de enlazar, podemos eliminar los shaders
        GL20.glDeleteShader(vertexShaderId);
        GL20.glDeleteShader(fragmentShaderId);
    }

    // Método para actualizar el desplazamiento del fondo (útil para fondos con efecto parallax)
    public void update(float deltaTime) {
        if (scrollSpeed > 0) {
            offsetX += scrollSpeed * deltaTime;

            // Resetear el offset para crear un efecto continuo
            if (offsetX > 1.0f) {
                offsetX = 0.0f;
            }
        }
    }

    public void setScrollSpeed(float speed) {
        this.scrollSpeed = speed;
    }

    public void render() {
        // Usar el shader program
        GL20.glUseProgram(shaderProgramId);

        // Actualizar el offset de textura en el shader
        int offsetLocation = GL20.glGetUniformLocation(shaderProgramId, "texOffset");
        GL20.glUniform2f(offsetLocation, offsetX, offsetY);

        // Activar la unidad de textura 0
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);

        // Configurar el sampler para usar la unidad de textura 0
        int textureSamplerLocation = GL20.glGetUniformLocation(shaderProgramId, "textureSampler");
        GL20.glUniform1i(textureSamplerLocation, 0);

        // Enlazar el VAO
        GL30.glBindVertexArray(vaoId);

        // Dibujar el rectángulo como 2 triángulos (un quad)
        GL11.glDrawArrays(GL11.GL_TRIANGLE_FAN, 0, 4);

        // Desenlazar el VAO
        GL30.glBindVertexArray(0);

        // Dejar de usar el shader program
        GL20.glUseProgram(0);
    }

    public void cleanup() {
        // Eliminar los VBOs
        GL15.glDeleteBuffers(vboId);
        GL15.glDeleteBuffers(texCoordsVboId);

        // Eliminar el VAO
        GL30.glDeleteVertexArrays(vaoId);

        // Eliminar la textura (si no usas TextureLoader)
        // GL11.glDeleteTextures(textureId);

        // Eliminar el shader program
        GL20.glDeleteProgram(shaderProgramId);
    }
}