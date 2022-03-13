import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;

public class JaydeASketch extends JPanel {

    private enum ControlStates {
        IDLE,               // No mouse button pressed
        DRAWING,            // LMB held, so moving the pen around
        PANNING,            // RMB held, so panning the image around
    }

    private enum AxisLock {
        NO_AXIS,
        VERTICAL_AXIS,
        HORIZONTAL_AXIS,
    }

    // Member Data ------------------------------------------------------------

    private final Dimension BOARD_SIZE;
    private final Color BOARD_COLOR;
    private final Color DRAW_COLOR;
    private final Color GRID_COLOR;
    private BufferedImage board;
    private BufferedImage compositeBuffer;
    private Graphics compositeBufferPen;
    private BufferedImage screenBuffer;
    private Graphics screenBufferPen;
    private Graphics boardPen;
    private final Point boardOffset;
    private final Color PEN_COLOR;
    private final Point penLocation;
    private Point mouseDragOrigin;
    private ControlStates controlState;
    private boolean axisLock;
    private AxisLock axisLockDir;
    private final int AXIS_LOCK_THRESHOLD;
    private Dimension axisLockAccumulator;
    private boolean fineControl;
    private final int FINE_CONTROL_LEVEL;
    private Dimension fineControlAccumulator;
    private boolean snapToGrid;
    private final int GRID_SIZE;
    private Dimension snapToGridAccumulator;
    private int zoomLevel;
    private final int ZOOM_MAX_LEVEL;

    // Member Methods ---------------------------------------------------------

    public JaydeASketch() {
        BOARD_SIZE = new Dimension(800,600);
        BOARD_COLOR = new Color(168,168,168);
        DRAW_COLOR = new Color(32,32,32);
        GRID_COLOR = new Color(152, 152, 152);
        boardOffset = new Point(0,0);
        PEN_COLOR = new Color(255,255,255);
        penLocation = new Point(BOARD_SIZE.width / 2, BOARD_SIZE.height / 2);
        controlState = ControlStates.IDLE;
        axisLock = false;
        axisLockDir = AxisLock.NO_AXIS;
        AXIS_LOCK_THRESHOLD = 5;
        axisLockAccumulator = new Dimension(0,0);
        fineControl = false;
        FINE_CONTROL_LEVEL = 5;
        fineControlAccumulator = new Dimension(0,0);
        snapToGrid = false;
        GRID_SIZE = 10;
        snapToGridAccumulator = new Dimension(0,0);
        zoomLevel = 1;
        ZOOM_MAX_LEVEL = 10;
    }

    private void dragPen(MouseEvent e) {
        Point penLocationPrev = new Point(penLocation);
        Point mouseDelta = calculateMouseDelta(e);
        penLocation.x += mouseDelta.x;
        penLocation.y += mouseDelta.y;
        mouseDragOrigin.x = e.getX();
        mouseDragOrigin.y = e.getY();

        // bounds checking
        if (penLocation.x < 0) { penLocation.x = 0; }
        if (penLocation.x > BOARD_SIZE.width) { penLocation.x = BOARD_SIZE.width -1; }
        if (penLocation.y < 0) { penLocation.y = 0; }
        if (penLocation.y > BOARD_SIZE.height) { penLocation.y = BOARD_SIZE.height -1; }

        boardPen.drawLine(penLocationPrev.x, penLocationPrev.y, penLocation.x, penLocation.y);

        paintComponent(getGraphics());
    }

    private void dragDrawingSurface(MouseEvent e) {
        Point mouseDelta = calculateMouseDelta(e);

        boardOffset.x += mouseDelta.x;
        boardOffset.y += mouseDelta.y;
        mouseDragOrigin.x = e.getX();
        mouseDragOrigin.y = e.getY();

        // bounds checking
        if (boardOffset.x > 0) {
            boardOffset.x = 0;
        }
        if ((boardOffset.x + (BOARD_SIZE.width * zoomLevel)) < BOARD_SIZE.width) {
            boardOffset.x = BOARD_SIZE.width - (BOARD_SIZE.width * zoomLevel);
        }
        if (boardOffset.y > 0) {
            boardOffset.y = 0;
        }
        if ((boardOffset.y + (BOARD_SIZE.height * zoomLevel)) < BOARD_SIZE.height) {
            boardOffset.y = BOARD_SIZE.height - (BOARD_SIZE.height * zoomLevel);
        }

        paintComponent(getGraphics());
    }

    private void shakeDrawingSurface() {
        boardOffset.x -= 8;
        boardOffset.y -= 1;
        paintComponent(getGraphics());

        try { Thread.sleep(80); }  catch (InterruptedException ignored) {  }

        boardOffset.x += 19;
        paintComponent(getGraphics());

        try { Thread.sleep(100); }  catch (InterruptedException ignored) {  }

        boardOffset.x -= 14;
        boardOffset.y += 2;
        paintComponent(getGraphics());

        try { Thread.sleep(90); }  catch (InterruptedException ignored) {  }

        boardOffset.x += 3;
        boardOffset.y -= 1;
        initDrawingSurfaces();
        paintComponent(getGraphics());
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
        // reduce how far the thing travels, and doing this before other
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
        // the direction without getting stuck due to lack of single-pixel mouse precision,
        // we'll ignore a few pixels of movement (AXIS_LOCK_THRESHOLD) while we accumulate
        // the deltas for those pixels.
        // When we have accumulated enough data, we choose which direction to lock based on
        // which direction the user moved the mouse the furthest
        // Accumulator resets when the user releases Shift, so we're locked until then
        if (axisLockDir == AxisLock.NO_AXIS) {
            axisLockAccumulator.width += Math.abs(delta.x);
            axisLockAccumulator.height += Math.abs(delta.y);

            if (Math.abs(axisLockAccumulator.width - axisLockAccumulator.height) >= AXIS_LOCK_THRESHOLD) {
                if (axisLockAccumulator.width < axisLockAccumulator.height)
                    axisLockDir = AxisLock.VERTICAL_AXIS;
                else
                    axisLockDir = AxisLock.HORIZONTAL_AXIS;
            } else {
                delta.x = 0;    // still accumulating data, ignore this movement
                delta.y = 0;
            }
        }

        if (axisLockDir == AxisLock.HORIZONTAL_AXIS)
            delta.y = 0;
        if (axisLockDir == AxisLock.VERTICAL_AXIS)
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

    private void resetZoom() {
        zoomLevel = 1;
        boardOffset.x = 0;
        boardOffset.y = 0;

        paintComponent(getGraphics());
    }

    private void increaseZoom(Point focus) {
        if (zoomLevel >= ZOOM_MAX_LEVEL) { return; }
        zoomLevel++;
        // calculate new center point
        boardOffset.x =  (BOARD_SIZE.width / 2) - (focus.x * zoomLevel);
        boardOffset.y = (BOARD_SIZE.height / 2) - (focus.y * zoomLevel);

        checkBounds();
        paintComponent(getGraphics());
    }

    private void decreaseZoom(Point focus) {
        if (zoomLevel == 1) { return; }
        zoomLevel--;
        // calculate new center point
        boardOffset.x =  (BOARD_SIZE.width / 2) - (focus.x * zoomLevel);
        boardOffset.y = (BOARD_SIZE.height / 2) - (focus.y * zoomLevel);

        checkBounds();
        paintComponent(getGraphics());
    }

    private void checkBounds() {
        // push back in-bounds if necessary
        if (boardOffset.x > 0) { boardOffset.x = 0; }
        if ((boardOffset.x + BOARD_SIZE.width * zoomLevel) < BOARD_SIZE.width) {
            boardOffset.x = BOARD_SIZE.width - (BOARD_SIZE.width * zoomLevel);
        }
        if (boardOffset.y > 0) { boardOffset.y = 0; }
        if ((boardOffset.y + BOARD_SIZE.height * zoomLevel) < BOARD_SIZE.height) {
            boardOffset.y = BOARD_SIZE.height - (BOARD_SIZE.height * zoomLevel);
        }
    }

    @Override
    public void paintComponent(Graphics g) {
        int boardSizeZoomedX = boardOffset.x + (BOARD_SIZE.width * zoomLevel);
        int boardSizeZoomedY = boardOffset.y + (BOARD_SIZE.height * zoomLevel);

        compositeBufferPen.drawImage(board,0, 0, null);

        compositeBufferPen.setColor(PEN_COLOR);
        compositeBufferPen.drawLine(penLocation.x, penLocation.y, penLocation.x, penLocation.y);

        screenBufferPen.drawImage(compositeBuffer,
                boardOffset.x, boardOffset.y, boardSizeZoomedX, boardSizeZoomedY,
                0, 0, BOARD_SIZE.width, BOARD_SIZE.height,
                null);

        if (zoomLevel > 6) { addGridLines(); }

        g.drawImage(screenBuffer, 0, 0, null);
    }

    private void addGridLines() {
        int rows = BOARD_SIZE.height / zoomLevel;
        int cols = BOARD_SIZE.width / zoomLevel;
        int rowOffset = (boardOffset.y % zoomLevel);
        int colOffset = (boardOffset.x % zoomLevel);
        screenBufferPen.setColor(GRID_COLOR);

        for (int row = 1; row <= rows; ++row) {
            int thisRow = (row * zoomLevel) + rowOffset;
            screenBufferPen.drawLine(0, thisRow, BOARD_SIZE.width, thisRow);
        }
        for (int col = 1; col <= cols; ++col) {
            int thisCol = (col * zoomLevel) + colOffset;
            screenBufferPen.drawLine(thisCol, 0, thisCol, BOARD_SIZE.height);
        }
    }

    private void processKeyTyped(KeyEvent e) {
        if (e.getKeyChar() == ' ') { shakeDrawingSurface(); }
        if (e.getKeyChar() == '0') { resetZoom(); }
        if (e.getKeyChar() == '+') { increaseZoom(new Point(penLocation.x, penLocation.y)); }
        if (e.getKeyChar() == '-') { decreaseZoom(new Point(penLocation.x, penLocation.y)); }
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
            axisLockDir = AxisLock.NO_AXIS;
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
        if (controlState != ControlStates.IDLE) { return; }
        if (e.getButton() == MouseEvent.BUTTON1) {
            controlState = ControlStates.DRAWING;
            mouseDragOrigin = new Point(e.getX(), e.getY());
        }
        if (e.getButton() == MouseEvent.BUTTON3) {
            controlState = ControlStates.PANNING;
            mouseDragOrigin = new Point(e.getX(), e.getY());
        }
        if (e.getButton() == MouseEvent.BUTTON2) {
            resetZoom();
        }
    }

    private void processMouseReleased(MouseEvent e) {
        if (controlState == ControlStates.DRAWING && e.getButton() == MouseEvent.BUTTON1) { controlState = ControlStates.IDLE; }
        if (controlState == ControlStates.PANNING && e.getButton() == MouseEvent.BUTTON3) { controlState = ControlStates.IDLE; }
    }

    private void processMouseDragged(MouseEvent e) {
        if (controlState == ControlStates.DRAWING) { dragPen(e); }
        if (controlState == ControlStates.PANNING) { dragDrawingSurface(e); }
    }

    private void processMouseWheelMoved(MouseWheelEvent e) {
        if (controlState != ControlStates.IDLE) { return; }
        if (e.getWheelRotation() < 0) { increaseZoom(new Point(penLocation.x, penLocation.y)); }
        if (e.getWheelRotation() > 0) { decreaseZoom(new Point(penLocation.x, penLocation.y)); }
    }

    public void initialize() {
        initPanel();
        initFrame();
        initDrawingSurfaces();
        initInterface();
        paintComponent(getGraphics());
    }

    private void initPanel() {
        setPreferredSize(BOARD_SIZE);
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

    private void initDrawingSurfaces() {
        screenBuffer = new BufferedImage(BOARD_SIZE.width, BOARD_SIZE.height, BufferedImage.TYPE_INT_RGB);
        screenBufferPen = screenBuffer.getGraphics();
        screenBufferPen.setColor(BOARD_COLOR);

        compositeBuffer = new BufferedImage(BOARD_SIZE.width, BOARD_SIZE.height, BufferedImage.TYPE_INT_RGB);
        compositeBufferPen = compositeBuffer.getGraphics();
        compositeBufferPen.setColor(BOARD_COLOR);

        board = new BufferedImage(BOARD_SIZE.width, BOARD_SIZE.height, BufferedImage.TYPE_INT_RGB);
        boardPen = board.getGraphics();
        boardPen.setColor(BOARD_COLOR);
        boardPen.fillRect(0,0, BOARD_SIZE.width, BOARD_SIZE.height);
        boardPen.setColor(DRAW_COLOR);
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
