import java.io.*;

/**
 * This is the class that students need to implement. The code skeleton is provided.
 * Students need to implement rtinit(), rtupdate() and linkhandler().
 * printdt() is provided to pretty print a table of the current costs for reaching
 * other nodes in the network.
 */ 
public class Node { 
    
    public static final int INFINITY = 9999;
    
    int[] lkcost;		/*The link cost between node 0 and other nodes*/
    int[][] costs;  		/*Define distance table*/
    int nodename;               /*Name of this node*/
    
    /* Class constructor */
    public Node() { }
    
    /* students to write the following two routines, and maybe some others */
    void rtinit(int nodename, int[] initial_lkcost) { 
        
        int mag = initial_lkcost.length;
        lkcost = initial_lkcost;
        costs = new int[mag][mag];
        this.nodename = nodename;
    
    	for (int a = 0; a < mag; a++) {
    		for (int b = 0; b < mag; b++) {
    			if (a == b) { 
                    costs[a][b] = initial_lkcost[a]; 
                }
    			else { 
                    costs[a][b] = INFINITY; 
                }					
    		}
    	}
    	System.out.printf("\nt=%.3f: Initialized node #%d\n", NetworkSimulator.clocktime, nodename);
        helper();
    }    
    
    void rtupdate(Packet rcvdpkt) { 
         
        boolean fwd = false;
        int[] distances = rcvdpkt.mincost;
        int origin = rcvdpkt.sourceid;
	    int cost = costs[origin][origin];

		for (int x = 0; x < 4; x++) {
		    if (x != nodename) {									
			    if (costs[x][origin] > cost + distances[x]) {	
				    costs[x][origin] = cost + distances[x];	
					fwd = true;
				}
			}
		}
		System.out.printf("t=%.3f: Node #%d received pkt from #%d\n", NetworkSimulator.clocktime, nodename, origin);
		
        if (fwd) { 
            System.out.println("Table change"); 
            }
		else { 
            System.out.println("NO table change"); 
            }
		printdt();
		
        if (fwd) { 
            helper(); 
        }
    }
    
    /* called when cost from the node to linkid changes from current value to newcost*/
    void linkhandler(int linkid, int newcost) { 
        
        int sub = newcost - costs[linkid][linkid];
        System.out.println("\nLinking of " + nodename + " and " + linkid + " is now " + newcost);   	
    
    	for (int x = 0; x < 4; x++) {
    		if (costs[x][linkid] != INFINITY) {
    			costs[x][linkid] += sub;
    		}
    	}
        helper();
     }   

	 private void helper() {
		
		boolean[] poison = {false, false, false, false};	
    	int[] mincost = {costs[0][0], costs[1][0], costs[2][0], costs[3][0]};
    	int[] path = {0, 0, 0, 0};

    	for (int i = 0; i < 4; i++) {
    		for (int j = 0; j < 4; j++) {
    			if (mincost[i] > costs[i][j]) {
    				mincost[i] = costs[i][j];
    				path[i] = j;    				
    			}
    		}
    	}
    	int[] mincopy = {mincost[0], mincost[1], mincost[2], mincost[3]};

    	for (int i = 0; i < 4; i++) {   		
    		for (int j = 0; j < 4; j++) { 
				poison[j] = false;
				mincopy[j] = mincost[j]; 
				}
    		if (costs[i][i] < INFINITY && i != nodename) {	 			
    			System.out.printf("\nt=%.3f: Node #%d sending pkt to #%d\n", NetworkSimulator.clocktime, nodename, i);
    			
    			for (int k = 0; k < 4; k++) {
    				if (path[k] == i) {
	    				poison[k] = true;
						mincopy[k] = INFINITY;	
	    				System.out.println("Link poisoned: shortest path to " + k + " includes " + i);
    				}
    			}   				
    			printhelper(mincopy, poison);
    			NetworkSimulator.tolayer2(new Packet(nodename, i, mincopy));
    		}
    	}
	} 

	private void printhelper(int[] arr, boolean[] poison) {
		
		System.out.print("Min distance to nodes: ");
		
		for (int x = 0; x < 4; x++) {		
			if (poison[x] == true) { 
				System.out.print(arr[x] + " is poisoned"); 
				}
			else { 
				System.out.print(arr[x]); 
				}
			if (x < 3) { 
				System.out.print(", "); 
				}
		}
		System.out.println();
	}

    /* Prints the current costs to reaching other nodes in the network */
    void printdt() {
        switch(nodename) {
	
	case 0:
	    System.out.printf("                via     \n");
	    System.out.printf("   D0 |    1     2 \n");
	    System.out.printf("  ----|-----------------\n");
	    System.out.printf("     1|  %3d   %3d \n",costs[1][1], costs[1][2]);
	    System.out.printf("dest 2|  %3d   %3d \n",costs[2][1], costs[2][2]);
	    System.out.printf("     3|  %3d   %3d \n",costs[3][1], costs[3][2]);
	    break;
	case 1:
	    System.out.printf("                via     \n");
	    System.out.printf("   D1 |    0     2    3 \n");
	    System.out.printf("  ----|-----------------\n");
	    System.out.printf("     0|  %3d   %3d   %3d\n",costs[0][0], costs[0][2],costs[0][3]);
	    System.out.printf("dest 2|  %3d   %3d   %3d\n",costs[2][0], costs[2][2],costs[2][3]);
	    System.out.printf("     3|  %3d   %3d   %3d\n",costs[3][0], costs[3][2],costs[3][3]);
	    break;    
	case 2:
	    System.out.printf("                via     \n");
	    System.out.printf("   D2 |    0     1    3 \n");
	    System.out.printf("  ----|-----------------\n");
	    System.out.printf("     0|  %3d   %3d   %3d\n",costs[0][0], costs[0][1],costs[0][3]);
	    System.out.printf("dest 1|  %3d   %3d   %3d\n",costs[1][0], costs[1][1],costs[1][3]);
	    System.out.printf("     3|  %3d   %3d   %3d\n",costs[3][0], costs[3][1],costs[3][3]);
	    break;
	case 3:
	    System.out.printf("                via     \n");
	    System.out.printf("   D3 |    1     2 \n");
	    System.out.printf("  ----|-----------------\n");
	    System.out.printf("     0|  %3d   %3d\n",costs[0][1],costs[0][2]);
	    System.out.printf("dest 1|  %3d   %3d\n",costs[1][1],costs[1][2]);
	    System.out.printf("     2|  %3d   %3d\n",costs[2][1],costs[2][2]);
	    break;
        }
    }
    
}
