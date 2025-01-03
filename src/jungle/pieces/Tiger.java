package jungle.pieces;
import jungle.Player;
import jungle.squares.Square;
/**
 * Special Piece Tiger
 * Behaviour: rank 6, only horizontal leaping
*/

public class Tiger extends Piece {
	//Has rank 6
	private static int TIGER_RANK = 6;
	public Tiger(Player owner, Square square) {
		super(owner, square, TIGER_RANK);
	}
	
	//Can leap horizontally only
	@Override
	public boolean canLeapHorizontally() {
		return true;	
	}
}
