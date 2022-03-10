import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;

public class JaydeASketch extends JPanel {

    private enum State {
        // Control states
        IDLE,               // No mouse button pressed
        DRAWING,            // LMB held, so moving the pen around
        PANNING,            // RMB held, so panning the image around

        // Axis lock
        NO_AXIS,
        VERTICAL_AXIS,
        HORIZONTAL_AXIS,
    }

    // Member Data ------------------------------------------------------------

    private final Dimension PANEL_SIZE;
    private final Color SURFACE_DEFAULT;
    private final Color DRAW_COLOR;
    private BufferedImage drawingSurface;
    private Graphics drawingSurfacePen;
    private final Point drawingSurfaceOffset;
    private Dimension drawingSurfaceZoomedSize;
    private BufferedImage penPointer;
    private final Color PEN_COLOR;
    private final Dimension PEN_POINTER_SIZE;
    private final Point penLocation;
    private Point mouseDragOrigin;
    private State controlState;
    private boolean axisLock;
    private State axisLockDir;
    private final int AXIS_LOCK_THRESHOLD;
    private Dimension axisLockAccumulator;
    private boolean fineControl;
    private final int FINE_CONTROL_LEVEL;
    private Dimension fineControlAccumulator;
    private boolean snapToGrid;
    private final int GRID_SIZE;
    private Dimension snapToGridAccumulator;

    // Member Methods ---------------------------------------------------------

    public JaydeASketch() {
        PANEL_SIZE = new Dimension(800,600);
        SURFACE_DEFAULT = new Color(168,168,168);
        DRAW_COLOR = new Color(32,32,32);
        drawingSurfaceOffset = new Point(0,0);
        drawingSurfaceZoomedSize = new Dimension(PANEL_SIZE.width, PANEL_SIZE.height);
        PEN_COLOR = new Color(255,255,255);
        PEN_POINTER_SIZE = new Dimension(4,4);
        penLocation = new Point(PANEL_SIZE.width / 2, PANEL_SIZE.height / 2);
        controlState = State.IDLE;
        axisLock = false;
        axisLockDir = State.NO_AXIS;
        AXIS_LOCK_THRESHOLD = 5;
        axisLockAccumulator = new Dimension(0,0);
        fineControl = false;
        FINE_CONTROL_LEVEL = 5;
        fineControlAccumulator = new Dimension(0,0);
        snapToGrid = false;
        GRID_SIZE = 50;
        snapToGridAccumulator = new Dimension(0,0);
    }

    private void dragPen(MouseEvent e) {
        Point mouseDelta = calculateMouseDelta(e);

        Point startingPenLocation = new Point(penLocation);
        penLocation.x += mouseDelta.x;
        penLocation.y += mouseDelta.y;
        mouseDragOrigin.x = e.getX();
        mouseDragOrigin.y = e.getY();

        if (penLocation.x < 0) { penLocation.x = 0; }
        if (penLocation.x > PANEL_SIZE.width) { penLocation.x = PANEL_SIZE.width; }
        if (penLocation.y < 0) { penLocation.y = 0; }
        if (penLocation.y > PANEL_SIZE.height) { penLocation.y = PANEL_SIZE.height; }

        drawingSurfacePen.drawLine(startingPenLocation.x, startingPenLocation.y, penLocation.x, penLocation.y);

        paintComponent(getGraphics());
    }

    private void dragDrawingSurface(MouseEvent e) {
        Point mouseDelta = calculateMouseDelta(e);
        Point drawingSurfaceOffsetOrigin = new Point(drawingSurfaceOffset);

        System.out.print("\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b");
        System.out.print("surface origin: " + drawingSurfaceOffsetOrigin.x + "," + drawingSurfaceOffsetOrigin.y);

        drawingSurfaceOffset.x += mouseDelta.x;
        drawingSurfaceOffset.y += mouseDelta.y;
        mouseDragOrigin.x = e.getX();
        mouseDragOrigin.y = e.getY();

        // bounds checking
        if (drawingSurfaceOffset.x > 0) {
            drawingSurfaceOffset.x = 0;
        }
        if ((drawingSurfaceOffset.x + drawingSurfaceZoomedSize.width) < PANEL_SIZE.width) {
            drawingSurfaceOffset.x = PANEL_SIZE.width - drawingSurfaceZoomedSize.width;
        }
        if (drawingSurfaceOffset.y > 0) {
            drawingSurfaceOffset.y = 0;
        }
        if ((drawingSurfaceOffset.y + drawingSurfaceZoomedSize.height) < PANEL_SIZE.height) {
            drawingSurfaceOffset.y = PANEL_SIZE.height - drawingSurfaceZoomedSize.height;
        }

        System.out.print(" new surface: " + drawingSurfaceOffset.x + "," + drawingSurfaceOffset.y);

        Point drawingSurfaceOffsetDelta = new Point();
        drawingSurfaceOffsetDelta.x = drawingSurfaceOffset.x - drawingSurfaceOffsetOrigin.x;
        drawingSurfaceOffsetDelta.y = drawingSurfaceOffset.y - drawingSurfaceOffsetOrigin.y;

        System.out.print(" delta: " + drawingSurfaceOffsetDelta.x + "," + drawingSurfaceOffsetDelta.y);

        penLocation.x += drawingSurfaceOffsetDelta.x;
        penLocation.y += drawingSurfaceOffsetDelta.y;

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

    private Point calculateMouseDelta(MouseEvent e) {
        Point mouseDelta = new Point(e.getX() - mouseDragOrigin.x, e.getY() - mouseDragOrigin.y);
        adjustForFineControl(mouseDelta);
        adjustForAxisLock(mouseDelta);
        adjustForSnapToGrid(mouseDelta);
        return mouseDelta;
    }

    private void adjustForFineControl(Point delta) {
        if (!fineControl) { return; }

        // The mouse delta is usually 1,0 or 0,1, so fine control will accumulate
        // deltas until we hit the FINE_CONTROL_LEVEL, and then we will move in
        // the direction accumulated and reset that direction.  This way we can
        // reduce how far the pointer travels, and doing this before other
        // controls will also reduce how fast they accumulate deltas.
        fineControlAccumulator.width += Math.abs(delta.x);
        fineControlAccumulator.height += Math.abs(delta.y);

        if (fineControlAccumulator.width > FINE_CONTROL_LEVEL) {
            fineControlAccumulator.width = 0;
        } else {
            delta.x = 0;
        }

        if (fineControlAccumulator.height > FINE_CONTROL_LEVEL) {
            fineControlAccumulator.height = 0;
        } else {
            delta.y = 0;
        }
    }

    private void adjustForAxisLock(Point delta) {
        if (!axisLock) { return; }

        // The mouse delta is usually 1,0 or 0,1, so to give the user a chance to choose
        // the direction without getting stuck due to single-pixel mouse precision,
        // we'll ignore a few pixels of movement (AXIS_LOCK_THRESHOLD) while we accumulate
        // the deltas for those pixels.
        // When we have accumulated enough data, we choose which direction to lock based on
        // which direction the user moved the mouse the furthest
        // Accumulator resets when the user releases Shift, so we're locked until then
        if (axisLockDir == State.NO_AXIS) {
            axisLockAccumulator.width += Math.abs(delta.x);
            axisLockAccumulator.height += Math.abs(delta.y);

            if (Math.abs(axisLockAccumulator.width - axisLockAccumulator.height) >= AXIS_LOCK_THRESHOLD) {
                if (axisLockAccumulator.width < axisLockAccumulator.height)
                    axisLockDir = State.VERTICAL_AXIS;
                else
                    axisLockDir = State.HORIZONTAL_AXIS;
            } else {
                delta.x = 0;    // still accumulating data, ignore this movement
                delta.y = 0;
            }
        }

        if (axisLockDir == State.HORIZONTAL_AXIS)
            delta.y = 0;
        if (axisLockDir == State.VERTICAL_AXIS)
            delta.x = 0;
    }

    private void adjustForSnapToGrid(Point delta) {
        if (!snapToGrid) { return; }

        // The mouse delta is usually 1,0 or 0,1, so snapping to grid will
        // accumulate deltas until we hit the GRID_SIZE for a direction,
        // and then we'll calculate the nearest imaginary grid point and snap
        // to that.
        snapToGridAccumulator.width += delta.x;
        snapToGridAccumulator.height += delta.y;

        if (Math.abs(snapToGridAccumulator.width) < GRID_SIZE) {
            delta.x = 0;
        } else {
            if (snapToGridAccumulator.width < 0) {
                delta.x = (GRID_SIZE + (penLocation.x % GRID_SIZE)) * -1;
            } else {
                delta.x = GRID_SIZE - (penLocation.x % GRID_SIZE);
            }
            snapToGridAccumulator.width = 0;
        }

        if (Math.abs(snapToGridAccumulator.height) < GRID_SIZE) {
            delta.y = 0;
        } else {
            if (snapToGridAccumulator.height < 0) {
                delta.y = (GRID_SIZE + (penLocation.y % GRID_SIZE)) * -1;
            } else {
                delta.y = GRID_SIZE - (penLocation.y % GRID_SIZE);
            }
            snapToGridAccumulator.height = 0;
        }

    }

    private void processKeyTyped(KeyEvent e) {
        if (e.getKeyChar() == ' ') { shakeDrawingSurface(); }
    }

    private void processKeyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ESCAPE) { System.exit(0); }
        if (e.getKeyCode() == KeyEvent.VK_SHIFT) { axisLock = true; }
        if (e.getKeyCode() == KeyEvent.VK_ALT) { fineControl = true; }
        if (e.getKeyCode() == KeyEvent.VK_CONTROL) { snapToGrid = true; }
    }

    private void processKeyReleased(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_SHIFT) {
            axisLock = false;
            axisLockDir = State.NO_AXIS;
            axisLockAccumulator = new Dimension(0,0);
        }
        if (e.getKeyCode() == KeyEvent.VK_ALT) {
            fineControl = false;
            fineControlAccumulator = new Dimension(0,0);
        }
        if (e.getKeyCode() == KeyEvent.VK_CONTROL) {
            snapToGrid = false;
            snapToGridAccumulator = new Dimension(0,0);
        }
    }

    private void processMousePressed(MouseEvent e) {
        if (controlState != State.IDLE) { return; }
        if (e.getButton() == MouseEvent.BUTTON1) {
            controlState = State.DRAWING;
            mouseDragOrigin = new Point(e.getX(), e.getY());
        }
        if (e.getButton() == MouseEvent.BUTTON3) {
            controlState = State.PANNING;
            mouseDragOrigin = new Point(e.getX(), e.getY());
        }
    }

    private void processMouseReleased(MouseEvent e) {
        if (controlState == State.DRAWING && e.getButton() == MouseEvent.BUTTON1) { controlState = State.IDLE; }
        if (controlState == State.PANNING && e.getButton() == MouseEvent.BUTTON3) { controlState = State.IDLE; }
    }

    private void processMouseDragged(MouseEvent e) {
        if (controlState == State.DRAWING) { dragPen(e); }
        if (controlState == State.PANNING) { dragDrawingSurface(e); }
    }

    private void processMouseWheelMoved(MouseWheelEvent e) {
        System.out.print("\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b");
        System.out.print("Mouse Wheel Moved: " + e.getWheelRotation());
    }

    @Override
    public void paintComponent(Graphics g) {
        g.drawImage(drawingSurface, drawingSurfaceOffset.x, drawingSurfaceOffset.y, null);
        g.drawImage(penPointer, penLocation.x - (PEN_POINTER_SIZE.width / 2), penLocation.y - (PEN_POINTER_SIZE.height / 2), null);
    }

    public void initialize() {
        initPanel();
        initFrame();
        initDrawingSurface();
        initPenPointer();
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
        frame.setBackground(Color.BLACK);
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

    private void initPenPointer() {
        penPointer = new BufferedImage(PEN_POINTER_SIZE.width,PEN_POINTER_SIZE.height,BufferedImage.TYPE_INT_RGB);
        Graphics g = penPointer.getGraphics();
        g.setColor(SURFACE_DEFAULT);
        g.fillRect(0,0,PEN_POINTER_SIZE.width,PEN_POINTER_SIZE.height);
        g.setColor(PEN_COLOR);
        g.fillOval(PEN_POINTER_SIZE.width / 2, PEN_POINTER_SIZE.height / 2, PEN_POINTER_SIZE.width, PEN_POINTER_SIZE.height);
        g.dispose();
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
