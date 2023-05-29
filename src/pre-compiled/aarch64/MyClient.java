import java.net.*;
import java.nio.Buffer;
import java.io.*;

public class MyClient {
    /**
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        try {
            // Initialization of variables for servers and job handling
            boolean isFlag = true;
            String largestServerType = null;
            int largestServerCores = 0;
            int largestServerID = 0;

            // Connection to the server
            Socket socket = new Socket("127.0.0.1", 50000);

            // Setup the input and output streams
            BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            DataOutputStream output = new DataOutputStream(socket.getOutputStream());

            // Read server response and store it in a variable
            String serverResponse = Receive(input);

            // Send the HELO message and the auth message
            Send(output, "HELO"); // Send HELO

            // Send "AUTH" to server to identify user
            Send(output, "AUTH");
            serverResponse = Receive(input); // Receive OK

            Send(output, "REDY");
            serverResponse = Receive(input);

            String[] jobInfo = serverResponse.split(" ");
            int jobValue = Integer.parseInt(jobInfo[2]);
            int jobCores = Integer.parseInt(jobInfo[4]);
            int jobMemory = Integer.parseInt(jobInfo[5]);
            int jobDisk = Integer.parseInt(jobInfo[6]);

            System.out.println("INFO: " + " Number: " +  jobValue + " " + " JobCores: " + jobCores + " " + " jobMemory: " + jobMemory + " " + " jobDisk: " + jobDisk);

            String[] serverList = serverResponse.split(" ");

            int serverNums = Integer.parseInt(serverList[1]);

            Send(output, "GETS Avail " + jobCores + " " + jobMemory + " " + jobDisk);
            serverResponse = Receive(input);

            if (!serverResponse.startsWith("OK")) {
                Send(output, "GETS Capable " + jobCores + " " + jobMemory + " " + jobDisk);
                serverResponse = Receive(input);
            }

            Send(output, "OK");

            int serverCount = Integer.parseInt(serverList[1]);
            int[] serverCores = new int[serverCount];
            int[] serverID = new int[serverCount];
            String[] serverType = new String[serverCount];

            // Number of servers that we are allocating jobs to
            int i = 0;
            while (i < serverNums) {
                serverResponse = Receive(input);
                String[] serverData = serverResponse.split(" ");

                serverID[i] = Integer.parseInt(serverData[1]);
                serverCores[i] = Integer.parseInt(serverData[4]);
                serverType[i] = serverData[0];

                if (isFlag && serverCores[i] > largestServerCores) {
                    largestServerType = serverType[i];
                    largestServerCores = serverCores[i];
                    largestServerID = serverID[i];
                    isFlag = false;
                }
                i++;
            }

            Send(output, "OK");

            // Implementation of bin packing using First Fit algorithm
            boolean isJobAllocated = false;
            int bestFitIndex = -1;
            int bestFitRemaining = Integer.MAX_VALUE;

            for (int j = 0; j < serverCount; j++) {
                if (serverCores[j] >= jobCores) {
                    int remainingCapacity = serverCores[j] - jobCores;
                    if (remainingCapacity < bestFitRemaining) {
                        bestFitIndex = j;
                        bestFitRemaining = remainingCapacity;
                    }
                }
            }

            if (bestFitIndex != -1) {
                Send(output, "SCHD " + jobValue + " " + serverType[bestFitIndex] + " " + serverID[bestFitIndex]);
                serverResponse = Receive(input);

                if (serverResponse.equals("OK")) {
                    isJobAllocated = true;
                }
            }

            // Schedule to the largest server with most cores if no suitable server found
            if (!isJobAllocated) {
                Send(output, "SCHD " + jobValue + " " + largestServerType + " " + largestServerID);
                serverResponse = Receive(input);
            }

            socket.close();
        } catch (Exception exception) {
            System.out.println(exception);
        }
    }
    
    // Helper function to send a message
    public static String Send(DataOutputStream output, String s) throws IOException{
        output.write(("" + s + "\n").getBytes());
        output.flush();
        return s;
    }

    // Helper function to receive a message
    public static String Receive(BufferedReader input) throws IOException{
        String mess = input.readLine(); 
        return mess; 
    }
}
