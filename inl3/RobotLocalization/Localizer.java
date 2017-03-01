import java.util.ArrayList;
import Jama.*;
public class Localizer implements EstimatorInterface {
	private int rows, cols, head;
	private int robot[];
	private Matrix tMat;
	private Matrix oMat;
	private Matrix fVect;
	private final double S1PROB = 0.05;
	private final double S2PROB = 0.025;

	public Localizer(int rows, int cols, int head) {
		this.rows = rows;
		this.cols = cols;
		this.head = head;
		int s = rows*cols*4;
		tMat = new Matrix(s,s,1.0/(s*s));
		oMat = new Matrix(s,s);
		fVect = new Matrix(s,1,1.0/s);

		robot = new int[]{(int) (Math.random()*rows), (int) (Math.random()*cols), (int) (Math.random()*head)};
	}

	/*
	 * return the number of assumed rows, columns and possible headings for the grid
	 * a number of four headings makes obviously most sense... the viewer can handle 
	 * four headings at maximum in a useful way.
	 */
	public int getNumRows() {
		return rows; //takes 1-n, not 0-(n-1)
	}
	
	public int getNumCols() {
		return cols; //takes 1-n, not 0-(n-1)
	}
	
	public int getNumHead() {
		return head; //takes 1-n, not 0-(n-1)
	}
	
	/*
	 * should trigger one step of the estimation, i.e., true position, sensor reading and 
	 * the probability distribution for the position estimate should be updated one step
	 * after the method has been called once.
	 */
	public void update() {
		walk(getLegalHeading()); //walk robot one step, t+1

		//sensor reading int[] {x, y}, t+1
		updateO();
		updateF(); //update f vector
	}
	
	/*
	 * returns the currently known true position i.e., after one simulation step
	 * of the robot as (x,y)-pair. 
	 */
	public int[] getCurrentTruePosition() {
		return new int[]{robot[0], robot[1]}; //Takes index 0-(n-1).
	}
	
	/*
	 * returns the currently available sensor reading obtained for the true position 
	 * after the simulation step 
	 */
	public int[] getCurrentReading() {
		double r = Math.random();
		int[] reading = getCurrentTruePosition();
		if (r<0.1) return reading;
		else if (0.10<r && r<=0.15  && inGrid(reading[0]-1,reading[1]+1)) return new int[]{reading[0]-1,reading[1]+1};
		else if (0.15<r && r<=0.20  && inGrid(reading[0]+1,reading[1]+1)) return new int[]{reading[0]+1,reading[1]+1};
		else if (0.20<r && r<=0.25  && inGrid(reading[0],reading[1]-1))   return new int[]{reading[0],reading[1]-1};
		else if (0.25<r && r<=0.275 && inGrid(reading[0]-2,reading[1]+2)) return new int[]{reading[0]-2,reading[1]+2};
		else if (0.275<r && r<=0.30 && inGrid(reading[0]-1,reading[1]+2)) return new int[]{reading[0]-1,reading[1]+2};
		else if (0.30<r && r<=0.325 && inGrid(reading[0],reading[1]+2))   return new int[]{reading[0],reading[1]+2};
		else if (0.325<r && r<=0.35 && inGrid(reading[0]+2,reading[1]+2)) return new int[]{reading[0]+2,reading[1]+2};
		else if (0.35<r && r<=0.375 && inGrid(reading[0]+2,reading[1]))   return new int[]{reading[0]+2,reading[1]};
		else if (0.375<r && r<=0.40 && inGrid(reading[0]-1,reading[1]-2)) return new int[]{reading[0]-1,reading[1]-2};

		reading = null;
		return reading;
	}

	/*
	 * returns the currently estimated (summed) probability for the robot to be in position
	 * (x,y) in the grid. The different headings are not considered, as it makes the 
	 * view somewhat unclear.
	 */
	public double getCurrentProb( int x, int y) {
		double prob = 0.0;
		int start = x*cols+y*4;
		for (int i=start; i<start+4; i++) {
			prob += fVect.get(i,0);
		}
		return prob;
	}

	/*
	 * returns the probability entry of the sensor matrices O to get reading r corresponding 
	 * to position (rX, rY) when actually in position (x, y) (note that you have to take 
	 * care of potentially necessary transformations from states i = <x, y, h> to 
	 * positions (x, y)). If rX or rY (or both) are -1, the method should return the probability for 
	 * the sensor to return "nothing" given the robot is in position (x, y).
	 * e_t = (rX, rY), X_t = i = (x, y), P(e_t | X_t)
	 */
	public double getOrXY( int rX, int rY, int x, int y) {
		return 0;
	}

	/*
	 * returns the probability entry (Tij) of the transition matrix T to go from pose 
	 * i = (x, y, h) to pose j = (nX, nY, nH)
	 */	
	public double getTProb( int x, int y, int h, int nX, int nY, int nH) {
		int i = x*rows + y*cols + h;
		int j = nX*rows + nY*cols + nH;
		//Jama matrix uses java indexing (0,1,2....n-1)!
		return tMat.get(i,j);
	}
	
	/*
	 * walk the robot one step in direction newh
	 */
	private void walk(int newh) {
		if 		(newh == 0) robot[0]--;
		else if (newh == 1) robot[1]++;
		else if (newh == 2) robot[0]++;
		else if (newh == 3) robot[1]--;
		
		robot[2] = newh;
	}

	private boolean inGrid (int x, int y) {
		if (x<0 || x>=rows || y<0 || y>=cols) return false;
		return true;
	}
	
	/*
	 * returns a heading randomly chosen from the true position's possible headings
	 */
	private int getLegalHeading() {
		boolean[] possMoves = possibleMoves(robot[0], robot[1]);
		int h = robot[2]; 
		int newh = robot[2];
		
		if (!possMoves[h] || (possMoves[h] && Math.random() < 0.3)) {
			ArrayList<Integer> trueHeadings = new ArrayList<Integer>();
			for (int i=0;i<4;i++) {
				if (possMoves[i]) trueHeadings.add(i);
			}

			newh = trueHeadings.get((int) (Math.random()*trueHeadings.size()));
		}
		return newh;
	}
	
	/*
	 * returns a boolean vector with the headings it is possible to move in 
	 * (i. e. no walls in that heading). [0] = N(orth), [1]=E, [2]=S, [3]=W
	 */
	private boolean[] possibleMoves(int x, int y) {
		return new boolean[]{inGrid(x-1,y),inGrid(x,y+1),inGrid(x+1,y),inGrid(x,y-1)};
	}

	//scales matrix so elements sums to 1
	private void scale(Matrix m) {
		double sum = 0;
		for (int i=0; i<m.getRowDimension(); i++) {
			for (int j=0; j<m.getColumnDimension(); j++) {
				sum += m.get(i,j);
			}
		}
		double alpha = 1.0/sum;
		m.times(alpha);
	}

	//returns alpha scalar, scaling matrix to 1
	private double alpha(Matrix m) {
		double sum = 0;
		for (int i=0; i<m.getRowDimension(); i++) {
			for (int j=0; j<m.getColumnDimension(); j++) {
				sum += m.get(i,j);
			}
		}
		double alpha = 1.0/sum;
		return alpha;
	}

	private void updateF() {
		fVect = scale( 
			oMat.times( 
				tMat.transpose().times( 
					fVect)));
	}

	private void updateO(int x, int y, int h) {
		oMat = new Matrix(rows*cols*4, rows*cols*4);
		int[] xDelta = new int[]{-2,-2,-2,-2,-2,-1,-1,-1,-1,-1,0,0,0,0,0,1,1,1,1,1,2,2,2,2,2};
		int[] yDelta = new int[]{-2,-1,0,1,2,-2,-1,0,1,2,-2,-1,0,1,2,-2,-1,0,1,2,-2,-1,0,1,2};
		double[] prob = new double[]{S2PROB,S2PROB,S2PROB,S2PROB,S2PROB,
									S2PROB,S1PROB,S1PROB,S1PROB,S2PROB,
									S2PROB,S1PROB,0.1,S1PROB,S2PROB,
									S2PROB,S1PROB,S1PROB,S1PROB,S2PROB,
									S2PROB,S2PROB,S2PROB,S2PROB,S2PROB};
		if (x != -1 && y != -1) {
			for (int lap=0;lap<xDelta.length;lap++) {		
				if (inGrid(x+xDelta[lap], y+yDelta[lap])) {
					int start = (x+xDelta[lap])*cols+(y+yDelta[lap])*4;
					for (int i=start; i<start+4; i++) {
						oMat.set(i,i,prob[lap]/4);
					}
				}
			}
		}
		//uses method nothingProbability to determine probability
		else {
			for (int i=0; i<rows; i++) {
				for (int j=0; j<cols; c++) {
					int start = (i*cols+j)*4;
					for (int s=start; s<start+4; i++) {
						double nonProb = nothingProbability(i,j);
						oMat.set(s,s,nonProb/4);
					}
				}
			}
		}


		/*
		//old code
		int[] xDelta = new int[]{-2, -2, -1, 0, 0, 0, 1, 1, 1, 2};
		int[] yDelta = new int[]{-2, 0, -1, -2, 0, 1, -2, -1, 2, -2};
		double[] prob = new double[]{S2PROB, S2PROB, S1PROB, S2PROB, 0.1, S1PROB, S2PROB, S1PROB, S2PROB, S2PROB};

		if (x != -1 && y != -1) {
			for (int lap=0;lap<xDelta.length;lap++) {		//absolute, heading-independent version
				if (inGrid(x+xDelta[lap], y+yDelta[lap])) {
					int start = (x+xDelta[lap])*cols+(y+yDelta[lap])*4;
					for (int i=start; i<start+4; i++) {
						oMat.set(i,i,prob[lap]/4);
					}
				}
			}
		} else {

			for (int r=0;r<rows;r++) {
				for (int c=0;c<cols;c++) {
					if ((r==0&&(c==0||c==cols-1) || (r==rows-1&&(c==0||c==cols-1)))) { //corners

					} else if ((r==1&&c==0) || (r==rows-2&&c==cols-1) || (r==rows-2&&(c==0||c==cols-1))) { 

					} else if (c==0||c==cols-1)
				}
			}
			for (int lap=0;lap<xDelta.length;lap++) {		//temporary
				if (inGrid(x+xDelta[lap], y+yDelta[lap])) {
					int start = (x+xDelta[lap])*cols+(y+yDelta[lap])*4;
					for (int i=start; i<start+4; i++) {
						oMat.set(i,i,1/(rows*cols));
					}
				}
			}
		}*/
	}

	/*
	*this method returns probability that the robot is
	*in a square, given that the sensor returns nothing.
	*/
	private double nothingProbability (int x, int y) {
	//perhaps define in constructor so no calcs needs to be made each time
	//must be in this order, this should make it correct for all n*m matrices??
	if ( wdX(x) >= 2 && wdY(y) >= 2 ) prob = (1-0.1-0.05*8-0.025*16); //middle

	if (wallDist(x)==0 && walldistY(y)==0) prob = (1-0.1-0.05*3-0.025*5); //corner
	if ((wallDist(x)==0 && walldistY(y)==1) || (wallDist(x)==1 && walldistY(y)==0)) prob = (1-0.1-0.05*5-0.025*6); //next to corner
	if (wallDist(x)==1 && walldistY(y)==1) prob = (1-0.1-0.05*8-0.025*7); //inner corner

	if ((wallDist(x)==0 && walldistY(y)>=2) || (wallDist(x)>=2 && walldistY(y)==0)) prob = (1-0.1-0.05*5-0.025*9); //top/bottom/left/right edge
	if ((wallDist(x)==1 && walldistY(y)>=2) || (wallDist(x)>=2 && walldistY(y)==1)) prob = (1-0.1-0.05*8-0.025*11); //inner top/bottom/left/right edge


	}

	public int wallDistX (int x) {
		return Math.min(rows-1-x,x);
	}
	
	public int wallDistY (int y) {
		return Math.min(cols-1-y,y);
	}
}