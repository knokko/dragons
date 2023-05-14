package dsl.pm2.interpreter

import dsl.pm2.interpreter.program.Pm2Program
import dsl.pm2.ui.Pm2SceneComponent
import graviks.glfw.GraviksWindow
import graviks2d.context.GraviksContext
import gruviks.glfw.createAndControlGruviksWindow
import org.lwjgl.vulkan.VK10.VK_MAKE_VERSION

private val program1 = """
    float minX = 2.0 * 0.1;
    float minY;
    float maxY = minY + 1.0;
    
    Vertex bottomLeft;
    bottomLeft.position = (minX, minY);
    
    Vertex bottomRight;
    bottomRight.position = (minX + 0.4, maxY);
    
    maxY = 1.3;
    
    Vertex topRight;
    topRight.position = (minX + 0.5, maxY);
    
    Vertex topLeft;
    topLeft.position = (minX, topRight.position.y);
    
    produceTriangle(bottomLeft, bottomRight, topRight);
    produceTriangle(topRight, topLeft, bottomLeft);
""".trimIndent()

private val program2 = """
    Vertex center;
    center.position = (0.5, 0.5);
    float radius = 0.2;
    
    int numParts = 100;
    for (0 <= part < numParts) {
        Vertex edge1;
        float angle1 = 360.0 * float(part) / float(numParts);
        edge1.position = (center.position.x + radius * cos(angle1), center.position.y + radius * sin(angle1));
        
        Vertex edge2;
        float angle2 = 360.0 * float(part + 1) / float(numParts);
        edge2.position = (center.position.x + radius * cos(angle2), center.position.y + radius * sin(angle2));
        
        produceTriangle(center, edge2, edge1);
    }
""".trimIndent()

private val program3 = """
    Vertex v;
    for (2 < whoops <= 8) {
        float test = float(whoops);
    }
    Vertex u;
""".trimIndent()

fun main() {
    val program = Pm2Program.compile(program2)
    val scene = program.run()

    val graviksWindow = GraviksWindow(
        800, 800, "DSL Playground", false, "Gruviks Tester",
        VK_MAKE_VERSION(0, 1, 0), true
    ) { instance, width, height ->
        GraviksContext(instance, width, height)
    }

    //createAndControlGruviksWindow(graviksWindow, Pm2SceneComponent(scene))
}
