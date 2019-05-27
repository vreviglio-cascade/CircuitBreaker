import com.google.gson.*;
import spark.Service;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;

import static spark.Spark.*;

public class Main {
    public static String urlStart = "http://localhost:4567/";

    public static void main(String[] args) {
        Service http = Service.ignite();
        http.port(8080);

        CircuitBreaker cb = new CircuitBreaker();

        http.get("/user/:id", (req, res) -> {
            res.type("application/json");
            String id = "0";
            if (cb.isCircuitAvailable()) {
                id = req.params("id");
                JsonObject user = readUrl(urlStart + "user/" + id);
                String statusCode = user.get("status").getAsString();
                cb.updateCircuit(statusCode);
                return new Gson().toJson(user);
            } else {
                id = req.params("id");
                String api = "/user";
                createLog(api);
                return new Gson().toJson(new StandardResponse(StatusResponse.ERROR, "API bloqueada. Intenté de nuevo en 5 segundos"));
            }
        });

    }

    public static JsonObject readUrl(String urlString) throws IOException {
        System.out.println(urlString);
        BufferedReader reader = null;
        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");
            if (connection.getResponseCode() == 200) {
                reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));
                char[] chars = new char[1024];
                StringBuffer buffer = new StringBuffer();
                int read = 0;
                while ((read = reader.read(chars)) != -1) {
                    buffer.append(chars, 0, read);
                }
                JsonParser parser = new JsonParser();
                JsonObject resultObj = (JsonObject) parser.parse(buffer.toString());
                return resultObj;
            } else {
                JsonElement res = new Gson().toJsonTree(new StandardResponse(StatusResponse.ERROR, "No se encontró el recurso al que intenta acceder"));
                return res.getAsJsonObject();
            }
        } catch (IOException e) {
            JsonElement res = new Gson().toJsonTree(new StandardResponse(StatusResponse.ERROR, "La API que intenta consumir esta caída"));
            return res.getAsJsonObject();

        } finally {
            if (reader != null) {
                reader.close();
            }
        }
    }

    public static void createLog(String api) {
        BufferedWriter writer = null;
        try {
            File file = new File("./circuit-breaker.log");
            if (!file.exists()) {
                file.createNewFile();
            }
            writer = new BufferedWriter(new FileWriter(file, true));
            Date fecha = new Date();
            String log = fecha.toString() + "\t" + "La API: \'" + api + "\' fue bloqueada";
            writer.write(log);
            writer.newLine();
            writer.close();
        } catch (IOException e) {
            System.out.println("No se pudo crear el archivo de log");
        }
    }
}
