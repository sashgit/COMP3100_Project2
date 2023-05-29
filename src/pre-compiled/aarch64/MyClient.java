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
            String serverResponse = input.readLine();


            // Send the HELO message and the auth message
            output.writeBytes("HELO\n"); // Send HELO
            output.flush();


            // Send "AUTH" to server to identify user
            output.write(("AUTH\n").getBytes());
            output.flush();
            serverResponse = input.readLine(); // Receive OK


            output.write(("REDY\n").getBytes());
            output.flush();


            serverResponse = input.readLine();


            String[] jobInfo = serverResponse.split(" ");
            int jobValue = Integer.parseInt(jobInfo[2]);
            int jobCores = Integer.parseInt(jobInfo[4]);
            int jobMemory = Integer.parseInt(jobInfo[5]);
            int jobDisk = Integer.parseInt(jobInfo[6]);


            System.out.println("INFO: " + " Number: " +  jobValue + " " + " JobCores: " + jobCores + " " + " jobMemory: " + jobMemory + " " + " jobDisk: " + jobDisk);


            String[] serverList = serverResponse.split(" ");

            int serverNums = Integer.parseInt(serverList[1]);

            output.write(("GETS Avail " + jobCores + " " + jobMemory + " " + jobDisk + "\n").getBytes());
            output.flush();
            serverResponse = input.readLine();


            if (!serverResponse.startsWith("OK")) {
                output.write(("GETS Capable " + jobCores + " " + jobMemory + " " + jobDisk + "\n").getBytes());
                output.flush();
                serverResponse = input.readLine();
            }


            output.write(("OK\n").getBytes());
            output.flush();


            int serverCount = Integer.parseInt(serverList[1]);
            int[] serverCores = new int[serverCount];
            int[] serverID = new int[serverCount];
            String[] serverType = new String[serverCount];


            // Number of servers that we are allocating jobs to
            int i = 0;
            while (i < serverNums) {
                serverResponse = input.readLine();
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


            output.write(("OK\n").getBytes());
            output.flush();


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
                output.write(("SCHD " + jobValue + " " + serverType[bestFitIndex] + " " + serverID[bestFitIndex] + "\n").getBytes());
                output.flush();
                serverResponse = input.readLine();


                if (serverResponse.equals("OK")) {
                    isJobAllocated = true;
                }
            }


            // Schedule to the largest server with most cores if no suitable server found
            if (!isJobAllocated) {
                output.write(("SCHD " + jobValue + " " + largestServerType + " " + largestServerID + "\n").getBytes());
                output.flush();
                serverResponse = input.readLine();
            }


            socket.close();
        } catch (Exception exception) {
            System.out.println(exception);
        }
    }

    public static String Send(DataOutputStream output, String s) throws IOException{
        output.write(("" + s + "\n").getBytes());
        output.flush();
        return s;
    }

    public static String Recieve(BufferedReader input) throws IOException{
        String mess = input.readLine(); 
        return mess; 
    }
}






