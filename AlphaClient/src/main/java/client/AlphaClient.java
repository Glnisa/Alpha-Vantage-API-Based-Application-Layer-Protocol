package client;


import java.io.*;
import java.net.*;
import java.net.Socket;


public class AlphaClient {

    private static final String address = "localhost";

    private static final int timeout = 120000; //milliseconds

    private static final int port_number = 8080;



    public static void main(String[] args) {

        try (Socket socket = new Socket(address, port_number)) {

            socket.setSoTimeout(timeout);

            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            BufferedReader input = new BufferedReader(new InputStreamReader(System.in));

            System.out.println("Connected to Alpha416 server. ");

            System.out.println("Enter commands ( for example Alpha416 EXC -from USD -to EUR -refresh ) ");

            String command;

            while (true) {

                System.out.print("Command: ");
                command = input.readLine();

                if(command.trim().equalsIgnoreCase("Alpha416 quit")){
                    System.out.println("Alpha416 ALPHA_200 Success");
                    System.out.println("Disconnecting from Alpha416 Multithreaded Server.");
                    break;
                }

                //sending command to Alpha416 Server
                out.println(command);


                //taking response of Alpha416 Server
                String line;
                StringBuilder response= new StringBuilder();
                boolean isContainsError = false;
                try{
                    while ((line = in.readLine()) != null) {

                        if(line.equalsIgnoreCase("end_of_response")){
                            break;
                        }
                        response.append(line).append("\n");

                        if(line.contains("timed out")){
                            System.out.println("Disconnecting from Alpha416 Multithreaded Server due to server timeout.");
                        }

                        if(line.contains("Error") || line.contains("Invalid")){
                            isContainsError = true;
                        }
                    }

                    System.out.println("Server response: "+response.toString().trim());

                    if(isContainsError){
                        System.out.println("Please try again with appropriate command, you may use 'help' command to see available commands.");
                    }


                } catch (SocketTimeoutException ex) {
                    System.out.println("Alpha416 ALPHA_500 Server Fault:"+ ex.getMessage());
                    break;
                }
            }
        } catch(SocketTimeoutException ex){
            System.out.println("Alpha416 ALPHA_500 Server Fault:"+ex.getMessage());

        }catch(IOException ex){
            System.out.println("Disconnecting from Alpha416 Multithreaded Server");
            System.out.println("Alpha416 ALPHA_500 Server Fault:"+ex.getMessage());


        }
    }



}
