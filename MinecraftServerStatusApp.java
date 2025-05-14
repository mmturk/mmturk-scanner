import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.JSONObject;

public class MinecraftServerStatusApp {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MinecraftServerStatusApp().createGUI());
    }

    public void createGUI() {
        JFrame frame = new JFrame("Minecraft Server Tool");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(700, 500);
        frame.setLayout(new BorderLayout());

        JLabel title = new JLabel("Development by mmturk", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 24));
        title.setForeground(Color.GREEN);
        title.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));

        JTabbedPane tabbedPane = new JTabbedPane();

        // Server Status tab
        JPanel serverPanel = new JPanel();
        serverPanel.setBackground(Color.DARK_GRAY);
        serverPanel.setLayout(new BoxLayout(serverPanel, BoxLayout.Y_AXIS));

        JTextField ipField = new JTextField();
        ipField.setMaximumSize(new Dimension(300, 30));
        JButton checkButton = new JButton("بررسی سرور");
        JTextArea resultArea = new JTextArea();
        resultArea.setEditable(false);
        resultArea.setBackground(Color.BLACK);
        resultArea.setForeground(Color.WHITE);
        resultArea.setFont(new Font("Monospaced", Font.PLAIN, 14));

        checkButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        ipField.setAlignmentX(Component.CENTER_ALIGNMENT);

        serverPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        serverPanel.add(ipField);
        serverPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        serverPanel.add(checkButton);
        serverPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        serverPanel.add(new JScrollPane(resultArea));

        tabbedPane.addTab("Server Status", serverPanel);

        checkButton.addActionListener((ActionEvent e) -> {
            String ip = ipField.getText().trim();
            if (!ip.isEmpty()) {
                String response = getServerInfo(ip);
                resultArea.setText(response);
            } else {
                JOptionPane.showMessageDialog(frame, "لطفاً IP سرور را وارد کنید.");
            }
        });

        // Port Scanner tab
        JPanel portPanel = new JPanel();
        portPanel.setLayout(new BoxLayout(portPanel, BoxLayout.Y_AXIS));
        portPanel.setBackground(Color.GRAY);

        JTextField ipScanField = new JTextField();
        JTextField startPortField = new JTextField();
        JTextField endPortField = new JTextField();
        JTextField timeoutField = new JTextField();
        JTextField threadsField = new JTextField();
        JButton scanButton = new JButton("شروع اسکن");
        JTextArea scanResultArea = new JTextArea();
        scanResultArea.setEditable(false);
        scanResultArea.setBackground(Color.BLACK);
        scanResultArea.setForeground(Color.GREEN);
        scanResultArea.setFont(new Font("Monospaced", Font.PLAIN, 13));

        ipScanField.setMaximumSize(new Dimension(300, 25));
        startPortField.setMaximumSize(new Dimension(300, 25));
        endPortField.setMaximumSize(new Dimension(300, 25));
        timeoutField.setMaximumSize(new Dimension(300, 25));
        threadsField.setMaximumSize(new Dimension(300, 25));

        portPanel.add(new JLabel("IP Address:"));
        portPanel.add(ipScanField);
        portPanel.add(new JLabel("Start Port:"));
        portPanel.add(startPortField);
        portPanel.add(new JLabel("End Port:"));
        portPanel.add(endPortField);
        portPanel.add(new JLabel("Timeout (ms):"));
        portPanel.add(timeoutField);
        portPanel.add(new JLabel("Threads:"));
        portPanel.add(threadsField);
        portPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        portPanel.add(scanButton);
        portPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        portPanel.add(new JScrollPane(scanResultArea));

        scanButton.addActionListener(e -> {
            String ip = ipScanField.getText().trim();
            int startPort, endPort, timeout, threads;
            try {
                startPort = Integer.parseInt(startPortField.getText().trim());
                endPort = Integer.parseInt(endPortField.getText().trim());
                timeout = Integer.parseInt(timeoutField.getText().trim());
                threads = Integer.parseInt(threadsField.getText().trim());
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(frame, "لطفاً مقادیر را به درستی وارد کنید.");
                return;
            }
            scanResultArea.setText("در حال اسکن...");
            new Thread(() -> {
                List<Integer> openPorts = scanOpenPorts(ip, startPort, endPort, timeout, threads);
                SwingUtilities.invokeLater(() -> {
                    StringBuilder result = new StringBuilder();
                    result.append("پورت‌های باز:").append("\n");
                    for (int port : openPorts) {
                        result.append("Port ").append(port).append(" is open\n");
                    }
                    if (openPorts.isEmpty()) {
                        result.append("هیچ پورتی باز نیست.");
                    }
                    scanResultArea.setText(result.toString());
                });
            }).start();
        });

        tabbedPane.addTab("Port Scanner", portPanel);

        frame.add(title, BorderLayout.NORTH);
        frame.add(tabbedPane, BorderLayout.CENTER);
        frame.setVisible(true);
    }

    public String getServerInfo(String ip) {
        try {
            URL url = new URL("https://api.mcsrvstat.us/2/" + ip);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                response.append(line);
            }
            in.close();

            JSONObject json = new JSONObject(response.toString());
            if (!json.getBoolean("online")) {
                return "سرور آفلاین است.";
            }

            StringBuilder info = new StringBuilder();
            info.append("وضعیت: آنلاین\n");
            info.append("MOTD: ").append(json.getJSONObject("motd").getJSONArray("clean").getString(0)).append("\n");
            info.append("IP: ").append(json.getString("ip")).append("\n");
            info.append("پورت: ").append(json.getInt("port")).append("\n");
            info.append("بازیکن‌ها: ").append(json.getJSONObject("players").getInt("online"))
                    .append("/").append(json.getJSONObject("players").getInt("max")).append("\n");
            info.append("نسخه: ").append(json.getString("version")).append("\n");

            return info.toString();
        } catch (Exception ex) {
            return "خطا در دریافت اطلاعات:\n" + ex.getMessage();
        }
    }

    public List<Integer> scanOpenPorts(String ip, int startPort, int endPort, int timeout, int threads) {
        List<Integer> openPorts = new ArrayList<>();
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        for (int port = startPort; port <= endPort; port++) {
            final int currentPort = port;
            executor.submit(() -> {
                try (Socket socket = new Socket()) {
                    socket.connect(new InetSocketAddress(ip, currentPort), timeout);
                    synchronized (openPorts) {
                        openPorts.add(currentPort);
                    }
                } catch (Exception ignored) {}
            });
        }
        executor.shutdown();
        while (!executor.isTerminated()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                break;
            }
        }
        return openPorts;
    }
}
