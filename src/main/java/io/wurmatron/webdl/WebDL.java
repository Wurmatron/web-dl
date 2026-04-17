package io.wurmatron.webdl;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

public class WebDL {
    // Config
    public static boolean media = true;
    public static boolean site = false;
    public static int delaySec = 15;
    public static File saveDir = new File("./webdl");
    public static String cookies = "";

    public static String mainURL;
    public static String baseURL;
    public static CookieManager manager = new CookieManager();

    public static void main(String[] args) throws IOException {
        // Forgor Args
        if (args.length == 0) {
            Scanner sc = new Scanner(System.in);
            System.out.print("Args: ");
            args = sc.nextLine().split(" ");
        }
        manager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
        CookieHandler.setDefault(manager);
        mainURL = processArgs(args);
        mainURL = formatURL(mainURL);
        sendInfo();
        String html = readSite(mainURL);
        if (site)
            save("test", "index.html", html);
        if (media)
            save("test", "images.txt", combine(media(html)));
    }

    public static String processArgs(String[] args) {
        String urlDetect = "";
        for (String arg : args) {
            if (arg.startsWith("-")) {
                // Media
                if (arg.trim().startsWith("-media=")) {
                    Boolean value = isTrueFalse(arg.substring(arg.indexOf("=") + 1));
                    if (value != null) {
                        media = value;
                    } else {
                        System.out.println("Invalid Argument Value '" + arg.substring(arg.indexOf("=") + 1) + "' for -media");
                    }
                } else if (arg.equalsIgnoreCase("-media")) {
                    media = true;
                }
                // Site
                if (arg.trim().startsWith("-site=")) {
                    Boolean value = isTrueFalse(arg.substring(arg.indexOf("=") + 1));
                    if (value != null) {
                        site = value;
                    } else {
                        System.out.println("Invalid Argument Value '" + arg.substring(arg.indexOf("=") + 1) + "' for -site");
                    }
                } else if (arg.equalsIgnoreCase("-site")) {
                    site = true;
                }
                // Delay
                if (arg.trim().startsWith("-delay=")) {
                    try {
                        int delay = Integer.parseInt(arg.substring(arg.indexOf("=") + 1));
                        if (delay > 0) {
                            delaySec = delay;
                        } else {
                            System.out.println("Delay must be greater than 0");
                        }
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid delay number: '" + arg.substring(arg.indexOf("=") + 1) + "'");
                    }
                } else if (arg.equalsIgnoreCase("-delay")) {
                    System.out.println("Delay needs a valid number -delay=15");
                }
                // Delay
                if (arg.trim().startsWith("-save=")) {
                    try {
                        File file = new File(arg.substring(arg.indexOf("=") + 1));
                        saveDir = file;
                    } catch (Exception e) {
                        System.out.println("Invalid save dir: '" + arg.substring(arg.indexOf("=") + 1) + "'");
                    }
                }
            } else {
                if (urlDetect.isBlank()) {
                    urlDetect = arg;
                } else {
                    System.out.println("Multiple possible url's detected! (" + urlDetect + ", " + arg + ")");
                    System.exit(-1);
                }

            }
        }
        return urlDetect.isBlank() ? null : urlDetect;
    }

    private static Boolean isTrueFalse(String val) {
        if (val.equalsIgnoreCase("true") || val.equalsIgnoreCase("yes") || val.equalsIgnoreCase("1") || val.equalsIgnoreCase("t") || val.equalsIgnoreCase("y")) {
            return true;
        } else if (val.equalsIgnoreCase("false") || val.equalsIgnoreCase("no") || val.equalsIgnoreCase("0") || val.equalsIgnoreCase("f") || val.equalsIgnoreCase("n")) {
            return false;
        }
        return null;
    }

    private static String formatURL(String url) {
        if (url.startsWith("http://") || url.startsWith("https://")) {
            return url;
        } else
            return String.format("https://%s", url);
    }

    private static void sendInfo() {
        System.out.println(" =-=-= URL: " + mainURL + " =-=-=");
        System.out.println("Download Media: " + media);
        System.out.println("Download Site: " + site);
        System.out.println("Delay Interval: " + delaySec + "sec");
        System.out.println("Cookies: " + cookies);
    }

    private static String readSite(String url) {
        try {
            URL site = new URL(url);
            HttpsURLConnection connect = (HttpsURLConnection) site.openConnection();
            connect.setRequestProperty("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/147.0.0.0 Safari/537.36");
            baseURL = site.getProtocol() + "://" + site.getHost();
            List<HttpCookie> siteCookies = manager.getCookieStore().get(new URI(url));
            HashMap<String, String> cookieTable = new HashMap<>();
            for (HttpCookie cookie : siteCookies)
                cookieTable.put(cookie.getName(), cookie.getValue());
            StringBuilder cookiebuilder = new StringBuilder();
            HashMap<String, String> requestedValues = new HashMap<>();
            for (String c : cookies.split(";"))
                requestedValues.put(c.substring(0, c.indexOf("=")), c.substring(c.indexOf("=") + 1));
            for (String key : cookieTable.keySet()) {
                if (requestedValues.containsKey(key)) {
                    cookiebuilder.append(requestedValues.get(key));
                } else {
                    cookiebuilder.append(key).append("=").append(cookieTable.get(key)).append(";");
                }
            }
            if (cookieTable.size() < requestedValues.size()) {
                for (String c : requestedValues.keySet())
                    cookiebuilder.append(c).append("=").append(requestedValues.get(c)).append(";");
            }
            connect.setRequestProperty("Cookie", cookiebuilder.toString());
            BufferedInputStream steam = new BufferedInputStream(connect.getInputStream());
            return readData(steam);
        } catch (Exception e) {
            System.out.println("Unable to read URL: " + url);
            System.err.println(e.getLocalizedMessage());
        }
        return null;
    }

    private static String readData(BufferedInputStream steam) throws IOException {
        StringBuilder data = new StringBuilder();
        int c;
        while ((c = steam.read()) != -1) {
            data.append((char) c);
        }
        return data.toString();
    }

    public static void save(String tag, String name, String data) throws IOException {
        File file = new File(saveDir, tag + File.separator + name);
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        Files.write(file.toPath(), data.getBytes(), StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
    }

    public static String[] media(String data) {
        List<String> images = new ArrayList<>();
        Document doc = Jsoup.parse(data);
        Elements img = doc.getElementsByTag("img");
        for (Element e : img) {
            String s = e.attribute("src").getValue();
            if(s.contains("base64"))
                images.add(s);
            if (s.startsWith("http:") || s.startsWith("https:")) {
                images.add(s);
            } else {
                images.add(baseURL + s);
            }
        }
        return images.toArray(new String[0]);
    }

    public static String combine(String[] arr) {
        StringBuilder sb = new StringBuilder();
        for (String s : arr) {
            sb.append(s).append("\n");
        }
        return sb.toString();
    }
}