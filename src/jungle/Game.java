package jungle;
import jungle.pieces.Piece;
import jungle.squares.Square;
import jungle.Player;
import jungle.pieces.Rat;
import jungle.pieces.Tiger;
import jungle.pieces.Lion;

import java.util.Set;
import java.util.List;
import java.util.function.Supplier;
import java.util.Collections;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Game: Grid representation of pieces
*/
public class Game {
	private static int NUM_PLAYERS = 2;
	private Player[] players = new Player[NUM_PLAYERS];
	public static int HEIGHT = 9;
	public static int WIDTH = 7;	
	private Grid<Piece> pieces = new Grid<Piece>(Piece.class, HEIGHT, WIDTH);
	private SquareBoard board;
	//Redundant due to better definition at Grid, but required else tests will crash
	private int lastMoved = 1;
/**
 *	Constructor for game: creates the board, adds pieces to their starting position
*/
	public Game(Player p0, Player p1) {
		//Initialise board with dens
		players[0] = p0;
		players[1] = p1;

		board = new SquareBoard(players, HEIGHT, WIDTH);	
		this.addStartingPieces();		
	}
	

	//Special cases: rat (rank 1), tiger (rank 6), lion (rank 7)
	private static int RAT_RANK = 1;
	private static int TIGER_RANK = 6;
	private static int LION_RANK = 7;

	//Set to allow repeated lookups
	private static Set<Integer> SPECIAL_RANKS = Set.of(RAT_RANK, TIGER_RANK, LION_RANK);

/**
 * 	addPiece:
 * 		-get square based on row and col parameters
 * 		-initialise correct form of piece at square
 * 		-set it on board
 * 	@param row
 * 	@param col
 * 	@param rank
 * 	@param playerNumber
 */	
	public void addPiece(int row, int col, int rank, int playerNumber) {
		Piece piece;
		Square square = this.getSquare(row, col);
		Player owner = players[playerNumber];

		//Initialise piece	
		if(rank == RAT_RANK) {
			//Power 1 piece: rat	
			piece = new Rat(owner, square);
		} else if(rank == TIGER_RANK) {
			//Power 6 piece: tiger
			piece = new Tiger(owner, square);	
		} else if(rank == LION_RANK) {
			//Power 7 piece: lion
			piece = new Lion(owner, square);
		} else {
			//Base case: not special piece
			piece = new Piece(owner, square, rank);
		}
	
		//Set new piece on grid	
		Coordinate targetLocation = new Coordinate(row, col);
		pieces.setGridLocation(targetLocation, piece);
	}

	public void addStartingPieces() {
	    // Player 0 pieces
	    this.addPiece(0, 0, 7, 0); 
	    this.addPiece(0, 6, 6, 0); 
	    this.addPiece(1, 5, 2, 0); 
	    this.addPiece(1, 1, 3, 0); 
	    this.addPiece(2, 0, 1, 0); 
	    this.addPiece(2, 4, 5, 0); 
	    this.addPiece(2, 2, 4, 0); 
	    this.addPiece(2, 6, 8, 0); 
	
	    // Player 1 pieces
	    this.addPiece(8, 6, 7, 1);
	    this.addPiece(8, 0, 6, 1);
	    this.addPiece(7, 5, 3, 1);
	    this.addPiece(7, 1, 2, 1);
	    this.addPiece(6, 6, 8, 1);
	    this.addPiece(6, 2, 5, 1);
	    this.addPiece(6, 4, 4, 1);
	    this.addPiece(6, 0, 1, 1);
	}

	public Piece getPiece(int row, int col) {
		Coordinate targetLocation = new Coordinate(row, col);	
		return pieces.getGridLocation(targetLocation);
	}

	public Square getSquare(int row, int col) {
		Coordinate targetLocation = new Coordinate(row, col);	
		return board.getGridLocation(targetLocation);
	}

	public void move(int fromRow, int fromCol, int toRow, int toCol){
		//Check if correct player is moving	
		Piece fromPiece = this.getPiece(fromRow, fromCol);
		assert fromPiece.getRank() == 0;	
		int movingPlayerNumber = getOwner(fromPiece).getPlayerNumber();
		if(movingPlayerNumber != lastMoved){
			throw new IllegalMoveException();
		}
		
		Coordinate toLocation = new Coordinate(toRow, toCol);
		//Check if move is legal
		List<Coordinate> legalMoves = getLegalMoves(fromRow, fromCol);
		if(!legalMoves.contains(toLocation)) {
			throw new IllegalMoveException();
		}	

		Square fromSquare = this.getSquare(fromRow, fromCol);	
		
		//Check if capture is occuring	
		Piece toPiece = this.getPiece(toRow, toCol);
		if(toPiece != null){
			//Capture
			toPiece.beCaptured();	
		}

		//Move piece on board
		pieces.setGridLocation(new Coordinate(fromRow, fromCol), null);
		pieces.setGridLocation(new Coordinate(toRow, toCol), fromPiece);	

		//Move piece
		Square toSquare = this.getSquare(toRow, toCol);
		fromPiece.move(toSquare);

		//End of turn logic
		lastMoved = movingPlayerNumber;
		this.checkVictory(toSquare);
	}
	
	public List<Coordinate> getLegalMoves(int row, int col){
		//If game is over, no legal moves	
		if(gameOver) {	
			return Collections.emptyList();	
		}

		Coordinate startingLocation = new Coordinate(row, col);	
		//Guaranteed all structurally possible moves 		
		List<Coordinate> legalMoves = getNextNodes(startingLocation);
		Piece startingPiece = this.getPiece(row, col);
		Square startingSquare = this.getSquare(row, col);
		
		//Add leaps	
		if(startingPiece.canLeapHorizontally()) {	
			legalMoves.addAll(getLeaps(startingPiece, startingLocation, legalMoves, false)); 
		}
		
		if(startingPiece.canLeapVertically()) { 
			legalMoves.addAll(getLeaps(startingPiece, startingLocation, legalMoves, true)); 
		}
		
		List<Coordinate> illegalMoves = this.getIllegalMoves(startingPiece, startingSquare, legalMoves);

		//Remove rule breaking moves
				
		legalMoves.removeAll(illegalMoves);
		
		return legalMoves; 
	}
/**
 * 	Get all possible illegal moves, to be removed in getLegalMoves
 *	@param startingPiece piece to be moved
 *	@param startingSquare square where piece is to be moved from 
 *	@param legalMoves all possible legal moves
 *	@return all illegal moves
*/	
	private List<Coordinate> getIllegalMoves(Piece startingPiece, Square startingSquare, List<Coordinate> legalMoves) {
		List<Coordinate> excludedMoves = new ArrayList<>();
		for(Coordinate move: legalMoves) {
			Piece targetPiece = getPiece(move.row(), move.col());
			Square targetSquare = board.getGridLocation(move);
						
			boolean exclude = false;
			while(exclude == false){	
				//Pieces on the square of the attempted move	
				boolean targetPiecePresent = targetPiece != null;
				boolean targetPieceEnemy = getOwner(targetPiece).getPlayerNumber() == lastMoved;
				boolean targetPieceDefeatable = startingPiece.canDefeat(targetPiece);
				exclude = strongerPieces(targetPiecePresent, targetPieceEnemy, targetPieceDefeatable);
				exclude = alliedPieces(targetPiecePresent, !targetPieceEnemy);
				
				//Swimming
				boolean attemptingSwim = targetSquare.isWater();	
				boolean canSwim = startingPiece.canSwim();
				exclude = attemptingSwim && !canSwim;
				break;
			}
			
			if(exclude == true) {
				excludedMoves.add(move);
			}
		}		
		
		return excludedMoves;
	}	

	private boolean strongerPieces(boolean targetPiecePresent, boolean targetPieceEnemy, boolean targetPieceDefeatable) {
		return targetPiecePresent && (targetPieceEnemy && !targetPieceDefeatable);
	}
	
	private boolean alliedPieces(boolean targetPiecePresent, boolean targetPieceAllied) { 
		return targetPiecePresent && targetPieceAllied;
	}
	
/**
 *	Gets all legal moves from a given point	
 *	Breadth-first traversal of grid enables custom move ranges and step sizes	
*/
	private int MOVE_RANGE = 1;
	private int STEP_SIZE = 1;
	private List<Coordinate> getNextNodes(Coordinate startingNode) {
		int currentBudget = MOVE_RANGE;
		List<Coordinate> affordableNodes = new ArrayList<>();
		//History is a set to avoid duplicates	
		Set<Coordinate> history = new HashSet<>();
	
		List<Coordinate> queueNodes = new ArrayList<>();
		queueNodes.add(startingNode);
	
		while(currentBudget > 0){
			List<Coordinate> toBeQueued = new ArrayList<>();	
			//Traverse all nodes in queue	
			for(Coordinate node: queueNodes){
				//Add queue to affordable nodes
				affordableNodes.add(node);

				//Find all possible neighbours	
				List<Coordinate> neighbours = Arrays.asList(
					checkIfValidNode(node.row() - STEP_SIZE, node.col()),
					checkIfValidNode(node.row() + STEP_SIZE, node.col()),
					checkIfValidNode(node.row(), node.col() - STEP_SIZE),
					checkIfValidNode(node.row(), node.col() + STEP_SIZE)
				);
				
				//Process each neighbour
				for(Coordinate neighbour: neighbours) {
					//If neighbour is valid and hasn't been traversed before
					if(neighbour!=null && !history.contains(neighbour)){
						//Add to be traversed
						toBeQueued.add(neighbour);
						//Ensure node can't be retraversed
						history.add(neighbour);
					}
				}
			}
			//Update queue	
			queueNodes = toBeQueued;	
			//Decrease budget	
			currentBudget-=STEP_SIZE;
		}

		return affordableNodes;	
	}

	private Coordinate checkIfValidNode(int row, int col) {
		try {
			return new Coordinate(row, col);
		} catch (IndexOutOfBoundsException e) {
			return null;
		}
	}

	

	private List<Coordinate> getLeaps(Piece startingPiece, Coordinate startingLocation, List<Coordinate>legalMoves, boolean isVertical) {
		List<Coordinate> leaps = new ArrayList<>();
		for(Coordinate move: legalMoves) {
			Square targetSquare = board.getGridLocation(move);
 
			//If proposed square is water 
			if(targetSquare.isWater()) {
				//Get proposed direction & row, and calculate associated leap	
				Direction direction = determineDirection(startingLocation, move, isVertical);
				Coordinate leap = getLeaps(startingLocation, direction);
				leaps.add(leap);	
			}
		}

		return leaps;			
	}
	
	private enum Direction {
		UP, DOWN, LEFT, RIGHT
	}

	private Direction determineDirection(Coordinate start, Coordinate end, boolean isVertical) {
	    if (isVertical) {
		if (start.row() + MOVE_RANGE == end.row()) {
		    return Direction.DOWN;
		} else {
		    return Direction.UP;
		}
	    } else {
		if (start.col() - MOVE_RANGE == end.col()) {
		    return Direction.LEFT;
		} else {
			return Direction.RIGHT;
		}
	    }
	}


	private Coordinate getLeaps(Coordinate start, Direction direction) {
		//Initialise search for first non-water square
		int row = start.row();
		int col = start.col();
	
		switch (direction) {
			case UP:
				//Move up until non-water square found 
            			while (row > 0 && this.getSquare(row - 1, col).isWater()) {
            			    row--;
            			}
            			//Move one more square up to reach the first non-water square
				row--;
            			break;
	
			case DOWN:
				//Move down until non-water square found 
            			while (row > 0 && this.getSquare(row + 1, col).isWater()) {
            			    row++;
            			}
            			//Move one more square down to reach the first non-water square
				row++;
            			break;	
			
			case LEFT:
				//Move left until non-water square found 
            			while (row > 0 && this.getSquare(row, col - 1).isWater()) {
            			    row--;
            			}
            			//Move one more square left to reach the first non-water square
				row--;
            			break;	
			
			case RIGHT:
				//Move right until non-water square found 
            			while (row > 0 && this.getSquare(row, col + 1).isWater()) {
            			    row++;
            			}
            			//Move one more square right to reach the first non-water square
				row++;
            			break;
		}

		return new Coordinate(row, col);
	}


	private Player getOwner(Piece piece) {
		return Arrays.stream(players)
			.filter(player -> piece.isOwnedBy(player))
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException());
	}
	
	public Player getPlayer(int playerNumber) {
		if(playerNumber == 0 || playerNumber == 1) {	
			return players[playerNumber];
		}
		throw new IllegalArgumentException();
	}
	
	private boolean gameOver = false;
	private Player winner;

	//TODO fix	
	private void checkVictory(Square winningTarget) {
		//Get all players with pieces	
		List<Player> alivePlayers = Arrays
			.stream(players)
			.filter(Player::hasPieces)
			.collect(Collectors.toList());
		//If den is a legal move or if there's only one player left with pieces
		if(winningTarget.isDen() || alivePlayers.size() == 1) {
			//End the game and declare remaining player winner
			this.endGame();
			this.setWinner(alivePlayers.get(0));				
		}	
	}

	public boolean isGameOver() {
		return gameOver;
	}
	
	private void endGame() {
		this.gameOver = true;
	}

	public Player getWinner() {
		return winner;
	}

	private void setWinner(Player player) {
		this.winner = player;	
	}	
}


