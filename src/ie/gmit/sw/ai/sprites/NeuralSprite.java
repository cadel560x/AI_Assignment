package ie.gmit.sw.ai.sprites;

import java.util.ArrayList;
import java.util.Random;

import ie.gmit.sw.ai.FuzzyLogic.Engageable;
import ie.gmit.sw.ai.Nodes.Player;
import ie.gmit.sw.ai.nn.SpiderCharacter;
import ie.gmit.sw.ai.node.Node;
import ie.gmit.sw.ai.traversers.AStarTraversator;
import ie.gmit.sw.ai.traversers.BasicHillClimbingTraversator;
import ie.gmit.sw.ai.traversers.DepthLimitedDFSTraversator;
import ie.gmit.sw.ai.traversers.Traversator;

public class NeuralSprite extends Sprite implements Runnable{

	private int feature;
	private Node[][] maze;
	private Object lock;
	private int row;
	private int col;
	private Node lastNode;
	private Node node = new Node(row,col,feature);;
	private boolean canMove;
	private Player player;
	private Node nextPosition;
	private double strength;
	private double health = 40;
	private int anger;
	Random random = new Random();
	private int counter;
	
	public NeuralSprite(String name, String... images) throws Exception {
		super(name, images);
	}
	public NeuralSprite(int row, int col, int feature, Object lock, Node[][] maze, Player player, int counter) throws Exception {
		this.row = row;
		this.col = col;
		this.feature =feature;
		this.player = player;
		this.strength = 2;
		node .setRow(row);
		node.setCol(col);
		node.setNodeType(feature);
		
		//Lock variable
		this.lock = lock;
		//Maze variable
		this.maze = maze;
		
		//Switch statement to check what type of spider
		//This determines the Traversator for each spider
		switch (node.getNodeType()) {
		case 10:
			anger = 8;//Sets the spider strength
			
			//This spider tries to find the player using AStarTraversator
			traversator = new AStarTraversator(player);
			break;
		case 11:
			//IDA not very good for controlling spiders - too slow
			//t = new IDAStarTraversator(player);
			anger = 4;//Sets the spider strength
			
			//This spider tries to find the player using BasicHillClimbingTraversator
			traversator= new BasicHillClimbingTraversator(player);
			break;
		case 12:
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
	
	public void engageNN(){
		//double healthy = 0;
		if(counter <= 0){
		SpiderCharacter enn = new SpiderCharacter();
		try {
			
			//SpiderHealthLogic
			//Spider health  (8 = Full Health, 4 = Injured , 2 = Close to death)
			double spiderHealth;
			if ( health >= 80.0)
				spiderHealth = 8;
			else if( health <= 50 && health > 20)
				spiderHealth = 4;
			else
				spiderHealth = 2;
			
			// EnemyWeapon
			// EnemyWeapon	(3 = Hydrogen Bomb ,2 = Bomb, 1 = Sword , 0 = None)
			double enemyWeapon;
			if(player.isHbomb()) //For H Bomb
				enemyWeapon = 3;
			else if(player.isBomb()) //For bomb
				enemyWeapon = 2;
			else if(player.isSword()) //For sword
				enemyWeapon = 1;
			else
				enemyWeapon = 0; //For no weapon
			
			
			//Proximity logic (not applicable for now, will be default)
			//Proximity ( 8 = Far Away, 4 = Nearby,  2 = Interacting)
			double proximity = 2;
			
			
			//	EnemyHealthLogic
			//	EnemyHealth (8  = Full Health, 4 = Half Health 2 = Close to death)
			double playerHealth;
			double currentHealth = player.getHealth();
			if ( currentHealth >= 80.0)
				playerHealth = 8;
			else if( currentHealth <= 50 && currentHealth > 20)
				playerHealth = 4;
			else
				playerHealth = 2;
			
			
			//Check the neural network for outcomes
			int action = enn.action(spiderHealth, enemyWeapon, proximity, playerHealth);
			if (action == 1){
				
				System.out.println("YOU ARE BEING ATTACKED BY A CHEEKY SPIDER !!!");
				//Use fuzzy logic to calculate the new player health
				Engageable ef= new Engageable();
				playerHealth = ef.engage(player.getWeapon(), this.strength, currentHealth );
				
				player.setHealth(playerHealth);
				health -= enemyWeapon * 40;
				//Player's new health
				System.out.println("Player health: " + playerHealth);
				if (health <= 0){
					node = new Node(row, col, -1);
					Thread.currentThread().stop();
				}
			}
			else if (action == 2) {
				System.out.println("The spider decided it is better to run away.");
				randomMove();
			}
			
		} catch (Exception e) {
			
			e.printStackTrace();
		}
		counter = 30;
		}
		counter --;
	}


	@Override
	public void run() {
		while(true){
			try {
				//Different sleep time per spider type
				Thread.sleep(500 * feature/2);
				//Find the path to take
				if(feature != 13){
					traverse(node.getRow(), node.getCol(), traversator);
				}
				// Move around the maze if within range
				if(node.getHeuristic(player) < 5){
					engageNN();
				}
				else if(canMove && node.getHeuristic(player) < 10  ){
					System.out.println("Searching for player");
					roam();
				}
				
				else {   
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

				Random random = new Random();
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
}
