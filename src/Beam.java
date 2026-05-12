class Beam {
    //direction constants
    private static final char NORTH = 'N';
    private static final char SOUTH = 'S';
    private static final char WEST = 'W';
    private static final char EAST = 'E';

    //grid position information
    final int num, row, col, length;
    final int[] direction; //direction vector (row, column)
    final int minCol, maxCol;

    /**
     * Constructs Beam with received id, grid coordinates, length and direction
     * also calculates the max and min column occupied by the beam
     * @param num unique 1-based identifier of the beam
     * @param row initial row coordinate
     * @param col initial column coordinate
     * @param length number of cells occupied by the beam
     * @param direction beam direction: 'N', 'S', 'E' or 'W'
     */
    Beam(int num, int row, int col, int length, char direction) {
        this.num = num;
        this.row = row;
        this.col = col;
        this.length = length;

        // Auxiliary variables
        int minC = col, maxC = col;
        switch (direction) {
            case NORTH -> this.direction = new int[]{-1, 0};
            case SOUTH -> this.direction = new int[]{1, 0};
            case WEST -> {
                minC = col - length + 1;
                this.direction = new int[]{0, -1};
            }
            case EAST -> {
                maxC = col + length - 1;
                this.direction = new int[]{0, 1};
            }
            default -> throw new IllegalArgumentException("Invalid direction: " + direction);
        }

        this.minCol = minC; // left most
        this.maxCol = maxC; // right most
    }

    /**
     * Checks if the beam intersects with received selection of columns
     * @param chosenStart left-most column
     * @param chosenSize amount of columns
     * @return true if beam overlaps the interval and must be removed
     */
    public boolean needsRemoval(int chosenStart, int chosenSize) {
        return this.maxCol >= chosenStart &&
                this.minCol <= (chosenStart + chosenSize - 1);
    }

    /**
     * @return vector pointing in the beams direction
     */
    public int[] getEscapeVector() {
        return direction;
    }

}