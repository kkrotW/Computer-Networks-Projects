import java.util.*;
import java.io.*;
import java.lang.System;

public class StudentNetworkSimulator extends NetworkSimulator
{
    /*
     * Predefined Constants (static member variables):
     *
     *   int MAXDATASIZE : the maximum size of the Message data and
     *                     Packet payload
     *
     *   int A           : a predefined integer that represents entity A
     *   int B           : a predefined integer that represents entity B 
     *
     * Predefined Member Methods:
     *
     *  void stopTimer(int entity): 
     *       Stops the timer running at "entity" [A or B]
     *  void startTimer(int entity, double increment): 
     *       Starts a timer running at "entity" [A or B], which will expire in
     *       "increment" time units, causing the interrupt handler to be
     *       called.  You should only call this with A.
     *  void toLayer3(int callingEntity, Packet p)
     *       Puts the packet "p" into the network from "callingEntity" [A or B]
     *  void toLayer5(String dataSent)
     *       Passes "dataSent" up to layer 5
     *  double getTime()
     *       Returns the current time in the simulator.  Might be useful for
     *       debugging.
     *  int getTraceLevel()
     *       Returns TraceLevel
     *  void printEventList()
     *       Prints the current event list to stdout.  Might be useful for
     *       debugging, but probably not.
     *
     *
     *  Predefined Classes:
     *
     *  Message: Used to encapsulate a message coming from layer 5
     *    Constructor:
     *      Message(String inputData): 
     *          creates a new Message containing "inputData"
     *    Methods:
     *      boolean setData(String inputData):
     *          sets an existing Message's data to "inputData"
     *          returns true on success, false otherwise
     *      String getData():
     *          returns the data contained in the message
     *  Packet: Used to encapsulate a packet
     *    Constructors:
     *      Packet (Packet p):
     *          creates a new Packet that is a copy of "p"
     *      Packet (int seq, int ack, int check, String newPayload)
     *          creates a new Packet with a sequence field of "seq", an
     *          ack field of "ack", a checksum field of "check", and a
     *          payload of "newPayload"
     *      Packet (int seq, int ack, int check)
     *          chreate a new Packet with a sequence field of "seq", an
     *          ack field of "ack", a checksum field of "check", and
     *          an empty payload
     *    Methods:
     *      boolean setSeqnum(int n)
     *          sets the Packet's sequence field to "n"
     *          returns true on success, false otherwise
     *      boolean setAcknum(int n)
     *          sets the Packet's ack field to "n"
     *          returns true on success, false otherwise
     *      boolean setChecksum(int n)
     *          sets the Packet's checksum to "n"
     *          returns true on success, false otherwise
     *      boolean setPayload(String newPayload)
     *          sets the Packet's payload to "newPayload"
     *          returns true on success, false otherwise
     *      int getSeqnum()
     *          returns the contents of the Packet's sequence field
     *      int getAcknum()
     *          returns the contents of the Packet's ack field
     *      int getChecksum()
     *          returns the checksum of the Packet
     *      int getPayload()
     *          returns the Packet's payload
     *
     */

    /*   Please use the following variables in your routines.
     *   int WindowSize  : the window size
     *   double RxmtInterval   : the retransmission timeout
     *   int LimitSeqNo  : when sequence number reaches this value, it wraps around
     */

    public static final int FirstSeqNo = 1;
    private int WindowSize;
    private double RxmtInterval;
    private int LimitSeqNo;
    
    // Add any necessary class variables here.  Remember, you cannot use
    // these variables to send messages error free!  They can only hold
    // state information for A or B.
    // Also add any necessary methods (e.g. checksum of a String)

    Packet ackPkt = new Packet(0,0,0);
    Packet[] buffer;
    double[] sentTimes;
    double[] ackTimes;
    LinkedList<Double> RTT = new LinkedList<Double>();
   
    private int sentPkts = 0;
    private int retransPkts = 0;
    private int rcvdPkts = 0;
    private int lostPkts = 0;
    private int corruptPkts = 0;
    private int nextSeqNo = FirstSeqNo;
    private int base = 1;
    private int bSeq = 1;

    protected int chksumCalc(int seq, int ack, String str) {
        int payload = str.hashCode();
        return seq + ack + payload;
    }

    // This is the constructor.  Don't touch!
    public StudentNetworkSimulator(int numMessages,
                                   double loss,
                                   double corrupt,
                                   double avgDelay,
                                   int trace,
                                   int seed,
                                   int winsize,
                                   double delay)
    {
        super(numMessages, loss, corrupt, avgDelay, trace, seed);
	WindowSize = winsize;
	LimitSeqNo = 2*winsize;
	RxmtInterval = delay;
    }

    // This routine will be called whenever the upper layer at the sender [A]
    // has a message to send.  It is the job of your protocol to insure that
    // the data in such a message is delivered in-order, and correctly, to
    // the receiving upper layer.
    protected void aOutput(Message message)
    {
    	String data = message.getData();
    	
        if (nextSeqNo < WindowSize + base) {
		 	int chkSum = chksumCalc(nextSeqNo, -1, data);
			Packet pkt = new Packet(nextSeqNo, -1, chkSum, data);
	    	System.out.println("aOutput recieved: " + pkt.toString());
			
            if (WindowSize < nextSeqNo) {
				System.out.println("Wrap around");
				buffer[nextSeqNo % LimitSeqNo] = pkt;
			}	
            else { 
                buffer[nextSeqNo] = pkt; 
            }			
            toLayer3(A, pkt);
			long time = System.nanoTime();
			System.out.println("Time sent: " + time);
			sentTimes[nextSeqNo] = time; 
            time = 0;
			sentPkts++;
			System.out.println("aOutput sent: " + pkt.toString());
			
            if (nextSeqNo == base) {
				startTimer(A, RxmtInterval);
				}
			nextSeqNo++;
    		}
    else return;
    }
    
    // This routine will be called whenever a packet sent from the B-side 
    // (i.e. as a result of a toLayer3() being done by a B-side procedure)
    // arrives at the A-side.  "packet" is the (possibly corrupted) packet
    // sent from the B-side.
    protected void aInput(Packet packet)
    {
		double time = System.nanoTime();
		System.out.println("Time ackd: "+ time);
        ackTimes[nextSeqNo] = time; 
        time = 0;
    	rcvdPkts++;
    	Packet pkt = new Packet(packet);
    	System.out.println("aInput: " + pkt.toString());
    	int seqNum = pkt.getSeqnum(); 
        int ackNum = pkt.getAcknum(); 
        int chkSum = pkt.getChecksum();
    	int chk = seqNum + ackNum;
  
    	if (chk == chkSum) {
    		base = 1 + pkt.getAcknum();
 
    		if (nextSeqNo == base) {
    			stopTimer(A);
    		}
    		else startTimer(A, RxmtInterval);
    	}
        else { 
            corruptPkts++; 
        }
    }
    
    // This routine will be called when A's timer expires (thus generating a 
    // timer interrupt). You'll probably want to use this routine to control 
    // the retransmission of packets. See startTimer() and stopTimer(), above,
    // for how the timer is started and stopped. 
    protected void aTimerInterrupt()
    {
    	startTimer(A, RxmtInterval);
    	
    	for (int i = base; i < nextSeqNo; i++) {
   
    		if (WindowSize < i) {
    	    	System.out.println("aTimerInterrupt: " + buffer[i % LimitSeqNo].toString());
    	    	System.out.println("Next seqno = " + nextSeqNo);
    	    	toLayer3(A, buffer[i % LimitSeqNo]);
    	    	sentPkts++; 
                retransPkts++;
    		}		
    		else { 
    	    	System.out.println("aTimerInterrupt: " + buffer[i].toString());
    			toLayer3(A, buffer[i]);
    	    	sentPkts++; 
                retransPkts++;
    			}
        }
    }
    
    // This routine will be called once, before any of your other A-side 
    // routines are called. It can be used to do any required
    // initialization (e.g. of member variables you add to control the state
    // of entity A).
    protected void aInit()
    {
		buffer = new Packet[LimitSeqNo];
		sentTimes = new double[1050];
        ackTimes = new double[1050];
    }
    
    // This routine will be called whenever a packet sent from the B-side 
    // (i.e. as a result of a toLayer3() being done by an A-side procedure)
    // arrives at the B-side.  "packet" is the (possibly corrupted) packet
    // sent from the A-side.
    protected void bInput(Packet packet)
    {
    	rcvdPkts++;
    	Packet pkt = new Packet(packet);
    	System.out.println("bInput recieved: " + pkt.toString());
    	System.out.println("Next seqno = " + nextSeqNo);
    	System.out.println("bSeq = " + bSeq);
    	int seqNum = pkt.getSeqnum(); 
        int ackNum = pkt.getAcknum(); 
        int chkSum = pkt.getChecksum();
    	String payload = pkt.getPayload();
    	int chk = chksumCalc(seqNum, ackNum, payload);
    	System.out.println("Expected checksum: " + chk);
    	int ackChk;
 
    	if ((chk == chkSum) && (bSeq == seqNum)) {
    		toLayer5(payload);
    		ackNum = seqNum;
    		ackChk = seqNum + ackNum;
    		ackPkt = new Packet(bSeq, ackNum, ackChk);
    		System.out.println("New ackPkt: " + ackPkt.toString());
    		toLayer3(B, ackPkt);
    		sentPkts++; 
            bSeq++;
    		System.out.println("New bSeq: " + bSeq);
    	}
    	else {
    		
            if (chk != chkSum) {
    			corruptPkts++;
    		}
    		if (ackPkt.getSeqnum() <= 0) {
    			return;
    		}
    		else {
	    		System.out.println("Last ackPkt: " + ackPkt.toString());
	    		toLayer3(B, ackPkt);
    		}
        }
    }
    
    // This routine will be called once, before any of your other B-side 
    // routines are called. It can be used to do any required
    // initialization (e.g. of member variables you add to control the state
    // of entity B).
    protected void bInit()
    {

    }

    // Use to print final statistics
    protected void Simulation_done()
    {
        double avgRTT;
        double sum = 0;
        lostPkts = sentPkts - rcvdPkts;

    	for (int i = 0; i < sentTimes.length - 1; i++) {
    		RTT.add(i, ackTimes[i] - sentTimes[i]);
    		sum += RTT.get(i);
    	}
    	avgRTT = sum / RTT.size();
    	System.out.println("Sent packets: " + sentPkts);
    	System.out.println("Retransmitted packets: " + retransPkts);
    	System.out.println("Received packets: " + rcvdPkts);
    	System.out.println("Lost packets: " + lostPkts);
    	System.out.println("Corrupt packets: " + corruptPkts);
        System.out.println("Average RTT: " + avgRTT + " ms");
    }	
}