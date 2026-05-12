class Beam {

    private static final char NORTH = 'N';
    private static final char SOUTH = 'S';
    private static final char WEST = 'W';
    private static final char EAST = 'E';

    final int num, row, col, length;
    final char direction;
    final int minRow, maxRow, minCol, maxCol;

    Beam(int num, int row, int col, int length, char direction) {
        this.num = num;
        this.row = row;
        this.col = col;
        this.length = length;
        this.direction = direction;

        // Auxiliary variables
        int minR = row, maxR = row, minC = col, maxC = col;
        switch (direction) {
            case NORTH ->
                    minR = row - length + 1;
            case SOUTH ->
                    maxR = row + length - 1;
            case WEST ->
                    minC = col - length + 1;
            case EAST ->
                    maxC = col + length - 1;
            default ->
                    throw new IllegalArgumentException("Invalid direction: " + direction);
        }

        // Ensure min <= max for rows and columns, makes it easier to check if a cell is taken by this beam.
        this.minRow = minR; // top most
        this.maxRow = maxR; // bottom most
        this.minCol = minC; // left most
        this.maxCol = maxC; // right most
    }

    public boolean needsRemoval(int chosenStart, int chosenSize) {
        int targetMaxCol = chosenStart + chosenSize - 1;
        return this.maxCol >= chosenStart && this.minCol <= targetMaxCol;
    }

    public int[] getEscapeVector() {
        return switch (this.direction) {
            case NORTH ->
                    new int[]{-1, 0};
            case SOUTH ->
                    new int[]{1, 0};
            case WEST ->
                    new int[]{0, -1};
            case EAST ->
                    new int[]{0, 1};
            default ->
                    null;
        };
    }

}