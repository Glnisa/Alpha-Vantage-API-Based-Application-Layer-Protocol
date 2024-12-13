package server;

import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.util.concurrent.atomic.AtomicInteger;


public class AlphaServer {

    private static final int port_number=8080;

    private static final String API_key = "RJ6AJMZN0C5QKLGH";

    private static final int timeout = 120000; //milliseconds

    private static final String url_api = "https://www.alphavantage.co//query";

    private static AtomicInteger request_counter = new AtomicInteger(0); //to keep track of requests to API




    public static void main(String[] args) {

        try(ServerSocket serverSocket = new ServerSocket(port_number)){
            System.out.println("Alpha is running on port: " + port_number);

            ExecutorService pool = Executors.newCachedThreadPool();

            while(true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("A new Client connected.");
                clientSocket.setSoTimeout(timeout);
                pool.execute(new ClientHandler(clientSocket));
            }

        }
        catch(IOException e) {
            System.out.println("Alpha416 ALPHA_500 Server Fault:"+ e.getMessage());

        }

    }


    static class ClientHandler extends Thread implements Runnable{

        private Socket clientSocket;



        public ClientHandler(Socket clientSocket) {
            //super();
            this.clientSocket = clientSocket;
        }



        @Override
        public void run() {
            // TODO Auto-generated method stub


            try(BufferedReader in =new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)){

                String request;
                while((request = in.readLine()) != null) {
                    if(request.trim().equalsIgnoreCase("Alpha416 quit")) {
                        out.println("Alpha416 ALPHA_200 Success\nDisconnected from Alpha416 Multithreaded server");
                        break;
                    }
                    System.out.println("Client's request:"+request);
                    String response= request_parser(request);
                    out.println(response);


                    if(response.contains("Error")){
                        out.println("Please try again");
                    }
                    out.flush(); //to be sure it is sent
                }


            } catch(SocketTimeoutException ex){
                System.out.println("Alpha416 ALPHA_500 Server Fault:"+ ex.getMessage());



            } catch(IOException e) {
                System.out.println("Alpha416 ALPHA_500 Server Fault: "+ e.getMessage());

            } finally{
                try{
                    clientSocket.close();
                } catch(IOException e){
                    System.out.println("Alpha416 ALPHA_500 Server Fault:"+ e.getMessage());
                }

            }

        }


        private String request_parser(String request) {

            String[] pieces = request.split(" ");

            //I added this command to see available commands
            if (pieces[0].equalsIgnoreCase("help")) {
                return "Use EXC after 'Alpha416' to get exchange rate between two currencies,\n" +
                        "given by mandatory parameters -from and -to\n     optional parameters: \n" +
                        "         -from_name : requests full name of from_currency name.\n" +
                        "         -to_name : requests full name of to_currency name.\n" +
                        "         -refresh : requests the last refreshed date fro the exchange rate.\n\n" +
                        "Use GAS after 'Alpha416' to get gas price data for a specific date.\n     optional parameters: \n" +
                        "         -change <date> : compares prices on two dates to indicate an increase or decrease." +
                        "         -average <date> : calculates the average price between two specified dates.\n\n" +
                        "Use -statistics after 'Alpha416' to see how much request that you sent to API.\n\n" +
                        "Use QUIT after 'Alpha416' to disconnect. \nend_of_response";


            }
            if (pieces.length < 2 || !pieces[0].equals("Alpha416")) {
                return "Alpha416 ALPHA_400 Invalid Request\n'Error' :Missing parameters.\nend_of_response";
            }

            String method_type = pieces[1];
            if (method_type.equalsIgnoreCase("QUIT")) {
                return "Alpha416 ALPHA_200 Success\nDisconnected from Alpha416 Multithreaded Server.\nend_of_response";
            }
            if (method_type.equalsIgnoreCase("GAS")) {
                boolean dateCheck= pieces.length>2 && pieces[2].equalsIgnoreCase("-date");
                if(!dateCheck) {
                    return "Alpha416 ALPHA_400 Invalid Request\n'Error' :Missing parameter for GAS method.\nend_of_response";
                }
                return handleGasPriceRequest(pieces);
            }
            if (method_type.equalsIgnoreCase("EXC")) {
                boolean fromCheck = pieces.length >2 &&pieces[2].equalsIgnoreCase("-from");
                boolean toCheck = pieces.length >4 &&pieces[4].equalsIgnoreCase("-to");

                if (!fromCheck || !toCheck) {
                    StringBuilder error = new StringBuilder();

                    error.append("Alpha416 ALPHA_400 Invalid Request\n'Error':Missing parameters for EXC method.");
                    if (!fromCheck) {
                        error.append("Missing -from parameter");
                    }
                    if (!toCheck) {
                        error.append("Missing -to parameter");
                    }
                    return error.toString().trim()+ "\nend_of_response";
                }
                return handleExchangeRateRequests(pieces);
            }
            //Added to see total requests to API, it is incremented in the fetchData
            if (method_type.equalsIgnoreCase("-statistics")) {
                return generateStats();
            }

            return "Alpha416 ALPHA_400 Invalid Request\n'Error':Unsupported parameters.\nend_of_response";
        }


        private String generateStats() {
            return "Alpha416 ALPHA200 Success\n" +
                    "Total requests: " + request_counter.get()+"\nend_of_response";
        }


        private String handleExchangeRateRequests(String[] pieces) {

            boolean from_name = false;

            boolean to_name = false;

            boolean refresh = false;

            String fromCurr = null;

            String toCurr = null;

            for (int i = 2; i < pieces.length; i++) {

                if(pieces[i].equalsIgnoreCase("-from_name")) {
                    from_name = true;
                }
                if(pieces[i].equalsIgnoreCase("-to_name")) {
                    to_name = true;
                }
                if(pieces[i].equalsIgnoreCase("-refresh")) {
                    refresh = true;
                }
                if(pieces[i].equalsIgnoreCase("-from")) {
                    fromCurr = (i+1 < pieces.length) ? pieces[i+1] : null;
                    i++;
                }
                if(pieces[i].equalsIgnoreCase("-to")) {
                    toCurr = (i+1 < pieces.length) ? pieces[i+1] : null;
                    i++;
                }


            }

            if(fromCurr == null || toCurr == null) {
                return "Alpha416 ALPHA_400 Invalid Request\n'Error':Currency parameters are missing, give currency after -from and -to parameters.\nend_of_response";
            }

            try {
                //url build for exchange
                String url =url_api + "?function=CURRENCY_EXCHANGE_RATE&from_currency=" + fromCurr+"&to_currency=" + toCurr + "&apikey=" + API_key;

                String returning_result = fetchData(url);
//                String mockData = """
//                    {
//                        "Realtime Currency Exchange Rate": {
//                            "1. From_Currency Code": "USD",
//                            "2. From_Currency Name": "United States Dollar",
//                            "3. To_Currency Code": "EUR",
//                            "4. To_Currency Name": "Euro",
//                            "5. Exchange Rate": "0.9325",
//                            "6. Last Refreshed": "2024-11-09 11:07:01",
//                            "7. Time Zone": "UTC",
//                            "8. Bid Price": "0.9323",
//                            "9. Ask Price": "0.9327"
//                        }
//                    }
//                    """;


                JSONObject json_f = new JSONObject(returning_result);

                if(json_f.has("Error message.")){
                    return "Alpha416 Alpha_404 Not Found\n'Error':Currency data not found.\nend_of_response";
                }

                //Dividing JSON file into pieces to get values and getting values
                JSONObject rate_data = json_f.getJSONObject("Realtime Currency Exchange Rate");

                String ex_Rate = rate_data.getString("5. Exchange Rate");

                String fromCurrName = rate_data.optString("2. From_Currency Name", "N/A");

                String toCurrName = rate_data.optString("4. To_Currency Name", "N/A");

                String last_refreshed = rate_data.getString("6. Last Refreshed");

                StringBuilder response = new StringBuilder("Alpha416 ALPHA_200 Success\nExchange rate: "+ex_Rate);

                //if user used -from_name parameter we also add info of -from currency's name
                if(from_name){
                    response.append(", From: ").append(fromCurrName);
                    from_name = false;
                }

                //if user used -to_name parameter we also add info of -from currency's name
                if(to_name){
                    response.append(", To: ").append(toCurrName);
                    to_name = false;
                }

                //if user -refresh we also add metainfo of last refreshed date of info
                if(refresh){
                    response.append(", Refresh: ").append(last_refreshed);
                    refresh = false;
                }

                response.append("\nend_of_response");

                return response.toString();


            }catch (Exception g){

                return "Alpha416 ALPHA_500 Server Fault\nend_of_response" ;

            }


        }


        private String handleGasPriceRequest(String[] pieces) {

            boolean average_calculation = false;

            String date = null;

            String date_compare = null;


            for(int i = 2; i < pieces.length; i++) {

                if(pieces[i].equals("-average") && i+1 < pieces.length) {

                    date_compare = pieces[i+1];
                    average_calculation = true;
                    i++;
                }
                else if(pieces[i].equals("-date") && i+1 < pieces.length) {
                    date = pieces[i+1];
                    i++;
                }
                else if(pieces[i].equals("-change") && i+1 < pieces.length) {
                    date_compare = pieces[i+1];
                    i++;
                }
            }
            if(date == null) {
                return "Alpha416 ALPHA_400 Invalid Request\n'Error':Missing date parameter.\nend_of_response";
            }

            try {
                //url build for gas price
                String url = url_api + "?function=NATURAL_GAS&interval=monthly&apikey=" + API_key;
                String returning_result = fetchData(url);
//                String mockData = """
//                        {
//                            "name": "Henry Hub Natural Gas Spot Price",
//                            "interval": "monthly",
//                            "unit": "dollars per million BTU",
//                            "data": [
//                                { "date": "2023-09-01", "value": "2.64" },
//                                { "date": "2023-08-01", "value": "2.58" },
//                                { "date": "2023-07-01", "value": "2.55" }
//                            ]
//                        }
//                        """;
                // get as JSON file
                JSONObject json_f = new JSONObject(returning_result);
                JSONArray array_data = json_f.getJSONArray("data");

                Double price = null;
                Double compare_price = null;

                for (int i = 0; i < array_data.length(); i++) {
                    //divide into pieces to get values and getting values
                    JSONObject obj_data = array_data.getJSONObject(i);
                    String date_data = obj_data.getString("date");
                    double value_in_date = obj_data.getDouble("value");

                    if (date_data.equals(date)) {
                        price = value_in_date;

                    }
                    if (date_compare != null && date_data.equals(date_compare)) {
                        compare_price = value_in_date;
                    }
                }
                if (price == null) {
                    return "Alpha416 ALPHA_404 Not Found\n'Error':Data is not available.\nend_of_response";
                }

                StringBuilder response = new StringBuilder("Alpha416 ALPHA_200 Success\nGas price on " + date + ":" + price);

                if (date_compare != null) {
                    if(compare_price == null) {
                        response.append("Alpha416 ALPHA_404 Not Found\n'Error':Date data to compare is not found.\nend_of_response");
                    }
                    else {
                        if (average_calculation) {
                            double average = (price + compare_price) / 2;
                            response.append(", Average price: ").append(average);
                        } else {
                            String change_in_price = (price > compare_price) ? "increased" : "decreased";
                            response.append(", Price ").append(change_in_price).append(" compared to ").append(date_compare);
                        }
                    }
                }

                response.append("\nend_of_response");


                return response.toString();

            }catch (Exception e){
                return "Alpha416 ALPHA_500 Server Fault\n'Error':Unable to fetch price of gas.\nend_of_response";
            }

        }


        private String fetchData(String url_s) throws IOException {

            //as my IDE suggested it, I used URI because it said URL is deprecated
            URI uri = URI.create(url_s);
            URL urlObj = uri.toURL();

            //open connection
            HttpURLConnection conn = (HttpURLConnection) urlObj.openConnection();
            conn.setRequestMethod("GET");

            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));

            String inputLine;

            StringBuilder fetchedData = new StringBuilder();

            while ((inputLine = in.readLine()) != null) {
                fetchedData.append(inputLine);
            }
            System.out.println(fetchedData);
            in.close();
            request_counter.incrementAndGet(); //increment the request counter for -statistics parameter
            return fetchedData.toString();

        }

    }
}

