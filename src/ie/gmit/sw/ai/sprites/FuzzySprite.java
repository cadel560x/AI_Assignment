package ie.gmit.sw.ai.sprites;

import java.util.ArrayList;
import java.util.Random;

import ie.gmit.sw.ai.FuzzyLogic.Engageable;
import ie.gmit.sw.ai.Nodes.Player;
import ie.gmit.sw.ai.node.Node;
import ie.gmit.sw.ai.traversers.AStarTraversator;
import ie.gmit.sw.ai.traversers.BasicHillClimbingTraversator;
import ie.gmit.sw.ai.traversers.DepthLimitedDFSTraversator;
import ie.gmit.sw.ai.traversers.Traversator;

public class FuzzySprite extends Sprite implements Runnable {

	private int feature;
	private Node[][] maze;
	private Object lock;
	private int row;
	private int col;
	private Node lastNode;
	private Node node = new Node(row,col,feature);
	private boolean canMove;
	private Player player;
	private Node nextPosition;
	private double anger;
	Random random = new Random();
	private int id;

	public FuzzySprite(String name, String... images) throws Exception {
		super(name, images);
	}

	public FuzzySprite(int row, int col, int feature, Object lock, Node[][] maze, Player player, int counter) throws Exception {
		this.row = row;
		this.col = col;
		this.feature =feature;
		this.player = player;
		node.setRow(row);
		node.setCol(col);
		node.setNodeType(feature);
		this.setId(counter);
		
		//Lock variable
		this.lock = lock;
		//Maze variable
		this.maze = maze;
		
		//Switch statement to check what type of spider
		//This determines the Traversator for each spider
		switch (node.getNodeType()) {
		case 6:
			anger = 8;//Sets the spider strength
			
			//This spider tries to find the player using AStarTraversator
			traversator = new AStarTraversator(player);
			break;
		case 7:
			//IDA not very good for controlling spiders - too slow
			//t = new IDAStarTraversator(player);
			anger = 4;//Sets the spider strength
			
			//This spider tries to find the player using BasicHillClimbingTraversator
			traversator= new BasicHillClimbingTraversator(player);
			break;
		case 8:
			anger = 2;//Sets the spider strength
			
			//This spider tries to find the player using DepthLimitedDFSTraversator
			traversator = new DepthLimitedDFSTraversator(10, player);
			break;
		default:
			//Set a random anger level between 1-10
			//This spider walks randomly around the maze
			anger = random.nextInt(10);
			break;
		}
	}

	@Override
	public void run() {
		while(true){
			try {
				//Different sleep time per spider type
				Thread.sleep(500 * feature/2);
				//Find the path to take
				if(feature != 9){
					traverse(node.getRow(), node.getCol(), traversator);
				}
				// Move around the maze if within range
				if(node.getHeuristic(player) <= 1){
					engage();
				}
				else if(canMove && node.getHeuristic(player) < 10){
					roam();     
				} else {    
					randomMove();       
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		
		}
	}
	//Roam around the map
		public void roam(){
			if(nextPosition != null){
				synchronized(lock){
					// Figure out all the nodes around
					Node[] surroundingNodes = node.adjacentNodes(maze);
					//List of empty surrounding nodes
					ArrayList<Node> emptySurroundingNodes = new ArrayList<>();
					// Check if they are empty
					for(Node n : surroundingNodes){
						if(n.getNodeType() == -1)
						{
							emptySurroundingNodes.add(n);
						}
					}

					// Check if they are empty
					for(Node n : emptySurroundingNodes){
						if(nextPosition.equals(n) )
						{		
							//New position of the object
							int newPositionX, newPositionY;
							//Previous position of the object
							int previousPositonX = node.getRow(), previousPositionY = node.getCol();

							System.out.println();
							newPositionX = nextPosition.getRow();
							newPositionY = nextPosition.getCol();

							node.setRow(newPositionX);
							node.setCol(newPositionY);

							maze[newPositionX][newPositionY] = node;
							maze[previousPositonX][previousPositionY] = new Node(previousPositonX, previousPositionY, -1);

							nextPosition = null;
							canMove = false;
							return;
						}	
					}
					// Move to random in empty
					randomMove();

					nextPosition = null;
					canMove = false;
					return;
				}
			}
			else{
				randomMove();

				canMove = false;
			}
		}
	private void randomMove() {
		synchronized(lock){
			// Figure out all the nodes around
			Node[] surroundingNodes = node.adjacentNodes(maze);
			//List of empty surrounding nodes
			ArrayList<Node> emptySurroundingNodes = new ArrayList<>();


			// Check if they are empty
			for(Node n : surroundingNodes){
				if(n.getNodeType() == -1 && n != lastNode)
				{
					emptySurroundingNodes.add(n);
				}
			}

			if(emptySurroundingNodes.size() > 0){

				
				int position = random.nextInt(emptySurroundingNodes.size());

				//New position of the object
				int newPositionX, newPositionY;
				//Previous position of the object
				int previousPositonX = node.getRow(), previousPositionY = node.getCol();
				newPositionX = emptySurroundingNodes.get(position).getRow();//nextPosition.getRow();
				newPositionY = emptySurroundingNodes.get(position).getCol();//nextPosition.getCol();
				node.setRow(newPositionX);
				node.setCol(newPositionY);

				lastNode = new Node(previousPositonX, previousPositionY, -1);
				maze[newPositionX][newPositionY] = node;
				maze[previousPositonX][previousPositionY] = lastNode;
			}
		}

	}
	public void traverse(int row, int col, Traversator t){
		t.traverse(maze, maze[row][col]);
		nextPosition = t.getNextNode();
		if(nextPosition != null){
			canMove = true;
		} else {
			canMove = false;
		}
	}
	
	public void engage(){
		Engageable e = new Engageable();
		double newHealth = e.engage(player.getWeapon(), anger, player.getHealth());
		System.out.println(newHealth);
		if(newHealth > 0){
			player.setHealth(newHealth);
			maze[node.getRow()][node.getCol()] = new Node(node.getRow(),node.getCol(),-1);
			Thread.currentThread().stop();
		}
		else if (newHealth < 1){
			player.setHealth(newHealth);
		}
		
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

}
