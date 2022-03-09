import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;

public class JaydeASketch extends JPanel {

    private enum State {
        // Control states
        IDLE,               // No mouse button pressed
        DRAWING,            // LMB held, so moving the pen around
//        PANNING,            // RMB held, so panning the image around
//
//        // Modifiers
//        AXIS_LOCK,          // Holding Shift while DRAWING or PANNING
//        FINE_CONTROL,       // Holding Alt while DRAWING or PANNING
//        SNAP_TO_GRID,       // Holding Ctrl while DRAWING or PANNING
    }

    // Member Data ------------------------------------------------------------

    private final Dimension PANEL_SIZE;

    private final Color SURFACE_DEFAULT;
    private final Color DRAW_COLOR;
    private BufferedImage drawingSurface;
    private Graphics drawingSurfacePen;
    private final Point drawingSurfaceOffset;
    private final Point penLocation;
    private Point mouseDragOrigin;

    private State controlState;

    // Member Methods ---------------------------------------------------------

    public JaydeASketch() {
        PANEL_SIZE = new Dimension(800,600);
        SURFACE_DEFAULT = new Color(168,168,168);
        DRAW_COLOR = new Color(32,32,32);
        drawingSurfaceOffset = new Point(0,0);
        penLocation = new Point(PANEL_SIZE.width / 2, PANEL_SIZE.height / 2);
        controlState = State.IDLE;
    }

    private void dragPen(MouseEvent e) {
        int mouseDeltaX = e.getX() - mouseDragOrigin.x;
        int mouseDeltaY = e.getY() - mouseDragOrigin.y;

        Point startingPenLocation = new Point(penLocation);
        penLocation.x += mouseDeltaX;
        penLocation.y += mouseDeltaY;
        mouseDragOrigin.x += mouseDeltaX;
        mouseDragOrigin.y += mouseDeltaY;

        if (penLocation.x < 0) { penLocation.x = 0; }
        if (penLocation.x > PANEL_SIZE.width) { penLocation.x = PANEL_SIZE.width; }
        if (penLocation.y < 0) { penLocation.y = 0; }
        if (penLocation.y > PANEL_SIZE.height) { penLocation.y = PANEL_SIZE.height; }

        drawingSurfacePen.drawLine(startingPenLocation.x, startingPenLocation.y, penLocation.x, penLocation.y);

        paintComponent(getGraphics());
    }

    private void shakeDrawingSurface() {
        drawingSurfaceOffset.x -= 8;
        drawingSurfaceOffset.y -= 1;
        paintComponent(getGraphics());

        try { Thread.sleep(80); }  catch (InterruptedException ignored) {  }

        drawingSurfaceOffset.x += 19;
        paintComponent(getGraphics());

        try { Thread.sleep(100); }  catch (InterruptedException ignored) {  }

        drawingSurfaceOffset.x -= 14;
        drawingSurfaceOffset.y += 2;
        paintComponent(getGraphics());

        try { Thread.sleep(90); }  catch (InterruptedException ignored) {  }

        drawingSurfaceOffset.x += 3;
        drawingSurfaceOffset.y -= 1;
        initDrawingSurface();

    }

    private void processKeyTyped(KeyEvent e) {
        if (e.getKeyChar() == ' ') { shakeDrawingSurface(); }
    }

    private void processKeyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ESCAPE) { System.exit(0); }
        System.out.print("\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b");
        System.out.print("Key Pressed: " + e.getKeyCode());
    }

    private void processKeyReleased(KeyEvent e) {
        System.out.print("\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b");
        System.out.print("Key Released: " + e.getKeyCode());
    }

    private void processMousePressed(MouseEvent e) {
        if (controlState != State.IDLE) { return; }
        if (e.getButton() == MouseEvent.BUTTON1) {
            controlState = State.DRAWING;
            mouseDragOrigin = new Point(e.getX(), e.getY());
        }
    }

    private void processMouseReleased(MouseEvent e) {
        if (controlState == State.DRAWING && e.getButton() == MouseEvent.BUTTON1) { controlState = State.IDLE; }
    }

    private void processMouseDragged(MouseEvent e) {
        if (controlState == State.DRAWING) { dragPen(e); }
    }

    private void processMouseWheelMoved(MouseWheelEvent e) {
        System.out.print("\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b");
        System.out.print("Mouse Wheel Moved: " + e.getWheelRotation());
    }

    public void initialize() {
        initPanel();
        initFrame();
        initDrawingSurface();
        initInterface();
    }

    private void initPanel() {
        setPreferredSize(PANEL_SIZE);
        setBackground(Color.BLACK);
    }

    private void initFrame() {
        JFrame frame = new JFrame("Jayde-A-Sketch");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);
        frame.add(this);
        frame.pack();
        frame.setVisible(true);

        requestFocusInWindow();
    }

    private void initDrawingSurface() {
        drawingSurface = new BufferedImage(PANEL_SIZE.width, PANEL_SIZE.height, BufferedImage.TYPE_INT_RGB);
        drawingSurfacePen = drawingSurface.getGraphics();
        drawingSurfacePen.setColor(SURFACE_DEFAULT);
        drawingSurfacePen.fillRect(0,0, PANEL_SIZE.width, PANEL_SIZE.height);
        drawingSurfacePen.setColor(DRAW_COLOR);

        paintComponent(getGraphics());
    }

    @Override
    public void paintComponent(Graphics g) {
        g.drawImage(drawingSurface, drawingSurfaceOffset.x, drawingSurfaceOffset.y, null);
    }

    private void initInterface() {
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                super.keyTyped(e);
                processKeyTyped(e);
            }

            @Override
            public void keyPressed(KeyEvent e) {
                super.keyPressed(e);
                processKeyPressed(e);
            }

            @Override
            public void keyReleased(KeyEvent e) {
                super.keyReleased(e);
                processKeyReleased(e);
            }
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                super.mousePressed(e);
                processMousePressed(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                super.mouseReleased(e);
                processMouseReleased(e);
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                super.mouseDragged(e);
                processMouseDragged(e);
            }
        });

        addMouseWheelListener(new MouseAdapter() {
            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                super.mouseWheelMoved(e);
                processMouseWheelMoved(e);
            }
        });

    }

    public static void main(String[] args) { SwingUtilities.invokeLater(JaydeASketch::createAndShowDisplay);}

    public static void createAndShowDisplay() {
        JaydeASketch jaydeASketch = new JaydeASketch();
        jaydeASketch.initialize();
    }
}
