class Beam {
    
    private final int num, row, col;
    private final char direction;
    private int minRow, maxRow, minCol, maxCol;

    // Direction constants
    private static final char NORTH = 'N';
    private static final char SOUTH = 'S';
    private static final char WEST = 'W';
    private static final char EAST = 'E';

    //auxiliary variables
    private int rowOrder, colOrder;

    /**
     * Factory method to create a Beam with automatically calculated boundaries.
     */
    public Beam(int num, int row, int col, int length, char direction) {
        this.num = num; 
        this.row = row;
        this.col = col;
        this.direction = direction;
        this.minCol = col; this.maxCol = col;
        this.minRow = row; this.maxRow = row;

        switch (direction) {
            case NORTH -> minRow = row - length + 1;
            case SOUTH -> maxRow = row + length - 1;
            case WEST  -> minCol = col - length + 1;
            case EAST  -> maxCol = col + length - 1;
            default    -> throw new IllegalArgumentException("Invalid direction: " + direction);
        }
    }

    // Custom helper methods

    /**
     * checks if this beam is horizontal or not
     * @return true is direction is east/west, false otherwise (south/north)
     */
    public boolean isHorizontal() {
        return direction == EAST || direction == WEST;
    }

    /**
     * checks if the beam points towards rows/columns with higher indexes
     * @return true if direction is south/east, false otherwise (north/west)
     */
    public boolean pointsForward() {
        return direction == SOUTH || direction == EAST;
    }

    /** Getters: */

    public int getMinRow() {
        return minRow;
    }

    public int getMaxRow() {
        return maxRow;
    }

    public int getMinCol() {
        return minCol;
    }

    public int getMaxCol() {
        return maxCol;
    }

    public int getCol() {
        return col;
    }

    public int getRow() {
        return row;
    }

    public int getID() {
        return num;
    }

    //note: the beam num is 1-based, their index is 0-based
    public int getIndex() {
        return num - 1;
    }

    public int getRowOrder() {
        return rowOrder;
    }

    public int getColOrder() {
        return colOrder;
    }

    /** Setters: */

    public void setRowOrder(int rowOrder) {
        this.rowOrder = rowOrder;
    }

    public void setColOrder(int colOrder) {
        this.colOrder = colOrder;
    }
}