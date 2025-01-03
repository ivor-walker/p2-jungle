package jungle;
import jungle.squares.Square;
import jungle.squares.PlainSquare;
import jungle.squares.Trap;
import jungle.squares.WaterSquare;
import jungle.squares.Den;

/**
 * SquareBoard: Grid representation of all the squares
*/
public class SquareBoard extends Grid<Square> {
/**
 *	Initialises board by setting all squares to plain
 *	Then adding the three types of special squares
*/
	public int[] WATER_ROWS = {3, 4, 5};
	public int[] WATER_COLS = {1, 2, 4, 5};
	
	public SquareBoard(Player[] players, int height, int width) {
		super(Square.class, height, width);	
		//Set the whole board to plainSquares (allRows inherited from grid)
		this.setGridLocation(getAllRows(), getAllCols(), PlainSquare::new);
		//Set water squares
		this.setGridLocation(WATER_ROWS, WATER_COLS, WaterSquare::new);

		//Set traps and dens for all players
		for(Player player: players) {
			this.addTrapsAndDen(player);
		}
	}

/**
 *	Set den and traps for a given player	
 *	@param player 
*/
	public int DEN_COL = 3;
	private int ROW_TRAP_PADDING = 1;
	private int COL_TRAP_PADDING = 1;
	private void addTrapsAndDen(Player player) {
		//Get column location of home	
		int[] targetCols = getSequence(DEN_COL - ROW_TRAP_PADDING, DEN_COL + COL_TRAP_PADDING + 1);
		int[] targetRows;
	
		//Get player number specific things	
		int denRow;
		Coordinate leftCorner;
		Coordinate rightCorner;
	
		//Player 0 case
		if(player.getPlayerNumber() == 0){
			//Den at top row
			denRow = 0;
			targetRows = getSequence(denRow + ROW_TRAP_PADDING + 1);
			//Corners at bottom left and right	
			leftCorner = new Coordinate(denRow + ROW_TRAP_PADDING, DEN_COL - COL_TRAP_PADDING);
			rightCorner = new Coordinate(denRow + ROW_TRAP_PADDING, DEN_COL + COL_TRAP_PADDING);


		//Player 1 case 
		} else if(player.getPlayerNumber() == 1){
			//Den at bottom row
			denRow = getHeight() - 1;
			targetRows = getSequence(denRow - ROW_TRAP_PADDING, denRow+1);
			//Corners at top left and right	
			leftCorner = new Coordinate(denRow - ROW_TRAP_PADDING, DEN_COL - COL_TRAP_PADDING);
			rightCorner = new Coordinate(denRow - ROW_TRAP_PADDING, DEN_COL + COL_TRAP_PADDING);
		} else {
			throw new IllegalArgumentException();
		}
	
		//Set a full rectangle of traps
		this.setGridLocation(targetRows, targetCols, () -> new Trap(player));
		//Locate and set den	
		Coordinate denCoordinate = new Coordinate(denRow, DEN_COL);
		this.setGridLocation(denCoordinate, new Den(player));
		//Remove corners
		this.setGridLocation(leftCorner, new PlainSquare());
		this.setGridLocation(rightCorner, new PlainSquare());
	}
}
