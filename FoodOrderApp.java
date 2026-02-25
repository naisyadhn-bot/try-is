package com.chatbotain.foodorderapp;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.text.NumberFormat;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

public class FoodOrderApp extends JFrame {

    // ================= BUYER =================
    JPanel buyerChatContainer;
    JScrollPane buyerChatScroll;
    JTextField buyerInput;
    FancyButton buyerSendBtn;
    JLabel buyerInfoBar;
    JFrame sellersFrame;

    // ================= SELLERS =================
    static class SellerUI {
        int id;
        JTextArea chat;
        JPanel requestContainer;
        JScrollPane chatScroll;
        SellerUI(int id){ this.id = id; }
    }
    List<SellerUI> sellers = new ArrayList<>();
    JLabel sellersInfoBar;

    // ================= TABLE =================
    JTable table;
    DefaultTableModel model;
    TableRowSorter<DefaultTableModel> sorter;

    // request tracking
    int requestIdCounter = 1;
    int lastRequestId = -1;

    // ================= CATALOG FILTER UI =================
    JTextField searchField;
    JCheckBox onlyAvailableCb;
    JCheckBox thisRequestOnlyCb;
    JComboBox<String> minRatingCb;
    JLabel catalogInfoBar;

    // recommendation card
    JLabel recTitle, recSub, recMeta;

    // ================= STYLES (CALM) =================
    final Font fontMain  = new Font("Segoe UI", Font.PLAIN, 13);
    final Font fontBold  = new Font("Segoe UI", Font.BOLD, 13);
    final Font fontTitle = new Font("Segoe UI", Font.BOLD, 15);

    final Color bgTop    = new Color(250, 252, 255);
    final Color bgBottom = new Color(246, 248, 252);

    final Color ink   = new Color(25, 28, 40);
    final Color muted = new Color(92, 98, 120);

    final Color accentA = new Color(96, 165, 250);   // blue
    final Color accentB = new Color(167, 243, 208);  // mint
    final Color pinkA   = new Color(244, 114, 182);  // calm pink
    final Color pinkB   = new Color(253, 186, 220);  // soft pink
    final Color purpleA = new Color(167, 139, 250);
    final Color purpleB = new Color(221, 214, 254);

    final Color headerLeft  = new Color(253, 230, 138);
    final Color headerRight = new Color(251, 207, 232);

    final Color tableOdd  = new Color(252, 252, 254);
    final Color tableEven = new Color(248, 250, 252);
    final Color tableSel  = new Color(229, 231, 235);

    final Color bubbleMeBg    = new Color(219, 234, 254);
    final Color bubbleOtherBg = new Color(243, 244, 246);

    final NumberFormat rupiah = NumberFormat.getCurrencyInstance(new Locale("id","ID"));
    final int SELLER_COUNT = 6;

    // AI weights
    final double W_RATING = 0.60;
    final double W_PRICE  = 0.40;

    final DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm");

    public FoodOrderApp() {
        try { UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel"); } catch(Exception ignored){}

        setTitle("AIN foodCatl");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1180, 720));

        for(int i=1;i<=SELLER_COUNT;i++) sellers.add(new SellerUI(i));

        buildUI();
        applyFilters();

        setLocationRelativeTo(null);
        setVisible(true);
    }

    // ================= ROOT UI (2 WINDOWS) =================
    void buildUI() {
        CalmBackground rootBg = new CalmBackground(bgTop, bgBottom);
        rootBg.setLayout(new BorderLayout());
        setContentPane(rootBg);

        JPanel buyer   = buildBuyerPanel();
        JPanel catalog = buildCatalogPanel();

        JSplitPane splitBuyerCatalog = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, buyer, catalog);
        splitBuyerCatalog.setResizeWeight(0.34);
        splitBuyerCatalog.setDividerSize(10);
        splitBuyerCatalog.setBorder(null);
        splitBuyerCatalog.setOpaque(false);

        buyer.setMinimumSize(new Dimension(310, 0));
        catalog.setMinimumSize(new Dimension(410, 0));

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.setBorder(new EmptyBorder(14,14,14,14));
        wrapper.add(buildTopRibbon(), BorderLayout.NORTH);
        wrapper.add(splitBuyerCatalog, BorderLayout.CENTER);

        rootBg.add(wrapper, BorderLayout.CENTER);

        openSellersWindow();
    }

    JPanel buildTopRibbon(){
        JPanel bar = new GlassCard(22, 0.72f);
        bar.setLayout(new BorderLayout(10,10));
        bar.setBorder(new EmptyBorder(12,14,12,14));

        JLabel title = new JLabel("AIN foodCatl");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(ink);

        JLabel subtitle = new JLabel("Multi-seller • Rating • AI Best • Cheapest");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subtitle.setForeground(muted);

        JPanel left = new JPanel(new GridLayout(2,1,0,2));
        left.setOpaque(false);
        left.add(title);
        left.add(subtitle);

        bar.add(left, BorderLayout.WEST);

        JPanel outer = new JPanel(new BorderLayout());
        outer.setOpaque(false);
        outer.setBorder(new EmptyBorder(0,0,12,0));
        outer.add(bar, BorderLayout.CENTER);
        return outer;
    }

    void openSellersWindow(){
        if(sellersFrame != null) return;

        JPanel sellerPanel = buildSellersPanel();

        sellersFrame = new JFrame("Sellers - AIN foodCatl");
        sellersFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        sellersFrame.setMinimumSize(new Dimension(520, 720));
        sellersFrame.setLocation(getX() + getWidth() + 12, getY());
        sellersFrame.setContentPane(sellerPanel);
        sellersFrame.setVisible(true);

        sellersFrame.addWindowListener(new WindowAdapter() {
            @Override public void windowClosed(WindowEvent e) {
                sellersFrame = null;
            }
        });
    }

    // ================= BUYER =================
    JPanel buildBuyerPanel() {
        JPanel body = new JPanel(new BorderLayout(10,10));
        body.setOpaque(false);

        buyerInfoBar = infoBar("Last request: -");

        buyerChatContainer = new JPanel();
        buyerChatContainer.setLayout(new BoxLayout(buyerChatContainer, BoxLayout.Y_AXIS));
        buyerChatContainer.setOpaque(false);

        buyerChatScroll = new JScrollPane(buyerChatContainer);
        styleScroll(buyerChatScroll);

        buyerChatScroll.getViewport().addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent e) {
                buyerChatContainer.revalidate();
                buyerChatContainer.repaint();
            }
        });

        buyerInput = input("Send request...");
        buyerSendBtn = new FancyButton("Send", accentA, accentB);
        buyerSendBtn.setEnabled(false);

        buyerInput.getDocument().addDocumentListener(new DocumentListener() {
            void update() {
                String t = buyerInput.getText().trim();
                buyerSendBtn.setEnabled(!t.isEmpty() && !t.equals("Send request..."));
            }
            public void insertUpdate(DocumentEvent e){ update(); }
            public void removeUpdate(DocumentEvent e){ update(); }
            public void changedUpdate(DocumentEvent e){ update(); }
        });

        buyerSendBtn.addActionListener(e -> sendMessage());
        buyerInput.addActionListener(e -> sendMessage());

        JPanel inputRow = new JPanel(new BorderLayout(8,8));
        inputRow.setOpaque(false);
        inputRow.add(buyerInput, BorderLayout.CENTER);
        inputRow.add(buyerSendBtn, BorderLayout.EAST);

        JPanel center = new JPanel(new BorderLayout(10,10));
        center.setOpaque(false);
        center.add(buyerInfoBar, BorderLayout.NORTH);
        center.add(wrapSoft(buyerChatScroll), BorderLayout.CENTER);

        body.add(center, BorderLayout.CENTER);
        body.add(inputRow, BorderLayout.SOUTH);

        return section("Buyer", body, accentA);
    }

    void sendMessage() {
        String text = buyerInput.getText().trim();
        if (text.isEmpty() || text.equals("Send request...")) return;

        int reqId = requestIdCounter++;
        lastRequestId = reqId;

        buyerInfoBar.setText("Last request: #" + lastRequestId);
        addBuyerBubble("You: " + text, true);

        for (SellerUI s : sellers) {
            s.chat.append("Buyer: " + text + "\n");
            s.requestContainer.add(createRequestForSeller(text, reqId, s));
            s.requestContainer.add(Box.createVerticalStrut(10));
            s.requestContainer.revalidate();
            s.requestContainer.repaint();
        }

        buyerInput.setText("");
        applyFilters();
        refreshCatalogInfo();
        refreshSellersInfo();
    }

    void addBuyerBubble(String text, boolean isMe) {
        String ts = LocalTime.now().format(timeFmt);

        JPanel line = new JPanel();
        line.setOpaque(false);
        line.setLayout(new BoxLayout(line, BoxLayout.X_AXIS));

        ChatBubble bubble = new ChatBubble(
                text, ts, isMe,
                isMe ? bubbleMeBg : bubbleOtherBg,
                fontMain,
                isMe ? accentA : new Color(156, 163, 175)
        );

        int viewportW = buyerChatScroll.getViewport().getWidth();
        int maxW = Math.max(220, (int)(viewportW * 0.72));
        bubble.setMaxBubbleWidth(maxW);

        if(isMe){
            line.add(Box.createHorizontalGlue());
            line.add(bubble);
        } else {
            line.add(bubble);
            line.add(Box.createHorizontalGlue());
        }

        buyerChatContainer.add(line);
        buyerChatContainer.add(Box.createVerticalStrut(8));
        buyerChatContainer.revalidate();
        buyerChatContainer.repaint();

        SwingUtilities.invokeLater(() -> {
            JScrollBar sb = buyerChatScroll.getVerticalScrollBar();
            sb.setValue(sb.getMaximum());
        });
    }

    static class ChatBubble extends JPanel {
        private final JTextArea message;
        private final JLabel time;
        private final boolean isMe;
        private final Color bgColor;
        private final Color tailAccent;
        private int maxBubbleWidth = 320;

        ChatBubble(String text, String ts, boolean isMe, Color bg, Font font, Color tailAccent) {
            this.isMe = isMe;
            this.bgColor = bg;
            this.tailAccent = tailAccent;

            setOpaque(false);
            setLayout(new BorderLayout());
            setBorder(new EmptyBorder(2,2,2,2));

            message = new JTextArea(text);
            message.setFont(font);
            message.setForeground(new Color(17, 24, 39));
            message.setOpaque(false);
            message.setEditable(false);
            message.setLineWrap(true);
            message.setWrapStyleWord(true);
            message.setBorder(new EmptyBorder(10,12,2,12));

            time = new JLabel(ts);
            time.setFont(font.deriveFont(Font.PLAIN, 11f));
            time.setForeground(new Color(107, 114, 128));
            time.setBorder(new EmptyBorder(0,12,8,12));
            time.setHorizontalAlignment(SwingConstants.RIGHT);

            add(message, BorderLayout.CENTER);
            add(time, BorderLayout.SOUTH);
        }

        void setMaxBubbleWidth(int w){
            this.maxBubbleWidth = w;
            message.setSize(new Dimension(maxBubbleWidth, Short.MAX_VALUE));
            revalidate();
            repaint();
        }

        @Override public Dimension getPreferredSize() {
            int w = Math.min(maxBubbleWidth, 560);
            message.setSize(new Dimension(w, Short.MAX_VALUE));
            Dimension md = message.getPreferredSize();
            Dimension td = time.getPreferredSize();

            int tailW = 10;
            int width = Math.max(md.width, td.width) + 18 + tailW;
            int height = md.height + td.height + 14;
            return new Dimension(width, height);
        }

        @Override public Dimension getMaximumSize() { return getPreferredSize(); }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int arc = 18, pad = 2, tailW = 10;
            int x = pad + (isMe ? 0 : tailW);
            int y = pad;
            int w = getWidth() - pad*2 - tailW;
            int h = getHeight() - pad*2;

            RoundRectangle2D rr = new RoundRectangle2D.Float(x, y, w, h, arc, arc);

            g2.setColor(new Color(0,0,0,12));
            g2.fill(new RoundRectangle2D.Float(x+1, y+2, w, h, arc, arc));

            g2.setColor(bgColor);
            g2.fill(rr);

            g2.setColor(new Color(0,0,0,18));
            g2.draw(rr);

            Polygon tail = new Polygon();
            int midY = y + 18;
            if(isMe){
                int tx = x + w;
                tail.addPoint(tx, midY);
                tail.addPoint(tx + tailW, midY + 6);
                tail.addPoint(tx, midY + 12);
            } else {
                int tx = x;
                tail.addPoint(tx, midY);
                tail.addPoint(tx - tailW, midY + 6);
                tail.addPoint(tx, midY + 12);
            }

            g2.setColor(bgColor);
            g2.fillPolygon(tail);
            g2.setColor(new Color(tailAccent.getRed(), tailAccent.getGreen(), tailAccent.getBlue(), 60));
            g2.drawPolygon(tail);

            g2.dispose();
            super.paintComponent(g);
        }
    }

    // ================= CATALOG =================
    JPanel buildCatalogPanel() {
        JPanel body = new JPanel(new BorderLayout(10,10));
        body.setOpaque(false);

        catalogInfoBar = infoBar("Menus shown: 0");
        JPanel filterBar = buildCatalogFilterBar();

        // ✅ FIX: kolom dibuat ulang -> Seller paling kiri, ReqID tetap ada tapi disembunyiin
        model = new DefaultTableModel(
                new String[]{"Seller","Menu","Price","Rating","Status","Action","ReqID"}, 0
        ) {
            public boolean isCellEditable(int row, int col) { return col == 5; } // Action column
        };

        table = new JTable(model);
        table.setFont(fontMain);
        table.setRowHeight(34);
        table.setShowVerticalLines(false);
        table.setIntercellSpacing(new Dimension(0, 8));
        table.setSelectionBackground(tableSel);
        table.setSelectionForeground(ink);
        table.setGridColor(new Color(0,0,0,0));
        table.setOpaque(false);

        sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);

        TableCellRenderer zebra = new ZebraCellRenderer(tableOdd, tableEven, ink, tableSel);
        for(int i=0; i<table.getColumnCount(); i++){
            table.getColumnModel().getColumn(i).setCellRenderer(zebra);
        }

        JTableHeader header = table.getTableHeader();
        header.setFont(fontBold);
        header.setReorderingAllowed(false);
        header.setPreferredSize(new Dimension(10, 34));
        header.setDefaultRenderer(new GradientHeaderRenderer(headerLeft, headerRight, ink));

        table.getColumn("Action").setCellRenderer(new BtnRender());
        table.getColumn("Action").setCellEditor(new BtnEditor());

        hideColumn("ReqID"); // ✅ tetep dipake buat filter, tapi ga keliatan

        setColWidth("Seller", 58);
        setColWidth("Rating", 62);
        setColWidth("Status", 90);
        setColWidth("Action", 94);

        JScrollPane tableScroll = new JScrollPane(table);
        styleScroll(tableScroll);

        table.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                int vr = table.getSelectedRow();
                if (vr == -1) return;
                int mr = table.convertRowIndexToModel(vr);
                if (String.valueOf(model.getValueAt(mr,4)).equals("Ready")) showInvoice(mr); // Status index = 4
            }
        });

        FancyButton aiBest = new FancyButton("AI Best", accentA, accentB);
        aiBest.addActionListener(e -> showAIBestCurrentReq());

        FancyButton cheapest = new FancyButton("Cheapest", purpleA, purpleB);
        cheapest.addActionListener(e -> showCheapestCurrentReq());

        JPanel actions = new JPanel(new GridLayout(1,2,10,10));
        actions.setOpaque(false);
        actions.add(aiBest);
        actions.add(cheapest);

        JPanel recCard = buildRecommendationCard();

        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.add(filterBar);
        top.add(Box.createVerticalStrut(8));
        top.add(catalogInfoBar);

        JPanel center = new JPanel(new BorderLayout(10,10));
        center.setOpaque(false);
        center.add(wrapSoft(tableScroll), BorderLayout.CENTER);

        JPanel bottom = new JPanel();
        bottom.setOpaque(false);
        bottom.setLayout(new BoxLayout(bottom, BoxLayout.Y_AXIS));
        bottom.add(actions);
        bottom.add(Box.createVerticalStrut(10));
        bottom.add(recCard);

        body.add(top, BorderLayout.NORTH);
        body.add(center, BorderLayout.CENTER);
        body.add(bottom, BorderLayout.SOUTH);

        SwingUtilities.invokeLater(() -> {
            applyFilters();
            refreshCatalogInfo();
        });

        return section("Catalog", body, accentA);
    }

    JPanel buildCatalogFilterBar(){
        JPanel bar = new GlassCard(18, 0.70f);
        bar.setLayout(new GridBagLayout());
        bar.setBorder(new EmptyBorder(10,10,10,10));

        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(4,4,4,4);
        gc.gridy = 0;
        gc.fill = GridBagConstraints.HORIZONTAL;

        JLabel sLbl = new JLabel("Search");
        sLbl.setFont(fontBold);
        sLbl.setForeground(ink);

        searchField = new JTextField();
        searchField.setFont(fontMain);
        searchField.setBorder(new CompoundBorder(new LineBorder(new Color(0,0,0,20), 1, true), new EmptyBorder(8,10,8,10)));
        searchField.setBackground(new Color(255,255,255));

        onlyAvailableCb = new JCheckBox("Only Available", true);
        onlyAvailableCb.setOpaque(false);
        onlyAvailableCb.setFont(fontMain);
        onlyAvailableCb.setForeground(ink);

        thisRequestOnlyCb = new JCheckBox("This Request Only", true);
        thisRequestOnlyCb.setOpaque(false);
        thisRequestOnlyCb.setFont(fontMain);
        thisRequestOnlyCb.setForeground(ink);

        JLabel rLbl = new JLabel("Min Rating");
        rLbl.setFont(fontBold);
        rLbl.setForeground(ink);

        minRatingCb = new JComboBox<>(new String[]{"Any","3.0+","3.5+","4.0+","4.5+","5.0"});
        minRatingCb.setFont(fontMain);

        searchField.getDocument().addDocumentListener(new DocumentListener() {
            void update(){ applyFilters(); }
            public void insertUpdate(DocumentEvent e){ update(); }
            public void removeUpdate(DocumentEvent e){ update(); }
            public void changedUpdate(DocumentEvent e){ update(); }
        });
        onlyAvailableCb.addActionListener(e -> applyFilters());
        thisRequestOnlyCb.addActionListener(e -> applyFilters());
        minRatingCb.addActionListener(e -> applyFilters());

        gc.gridx = 0; gc.weightx = 0; bar.add(sLbl, gc);
        gc.gridx = 1; gc.weightx = 1.0; bar.add(searchField, gc);
        gc.gridx = 2; gc.weightx = 0; bar.add(onlyAvailableCb, gc);
        gc.gridx = 3; gc.weightx = 0; bar.add(thisRequestOnlyCb, gc);
        gc.gridx = 4; gc.weightx = 0; bar.add(rLbl, gc);
        gc.gridx = 5; gc.weightx = 0; bar.add(minRatingCb, gc);

        return bar;
    }

    JPanel buildRecommendationCard(){
        JPanel card = new GlassCard(18, 0.70f);
        card.setLayout(new BorderLayout(8,8));
        card.setBorder(new EmptyBorder(10,12,10,12));

        JLabel t = new JLabel("Recommendation");
        t.setFont(fontTitle);
        t.setForeground(ink);

        recTitle = new JLabel("—");
        recTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        recTitle.setForeground(ink);

        recSub = new JLabel("Run AI Best / Cheapest to see result here.");
        recSub.setFont(fontMain);
        recSub.setForeground(muted);

        recMeta = new JLabel(" ");
        recMeta.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        recMeta.setForeground(new Color(107, 114, 128));

        JPanel mid = new JPanel();
        mid.setOpaque(false);
        mid.setLayout(new BoxLayout(mid, BoxLayout.Y_AXIS));
        mid.add(recTitle);
        mid.add(Box.createVerticalStrut(4));
        mid.add(recSub);
        mid.add(Box.createVerticalStrut(6));
        mid.add(recMeta);

        card.add(t, BorderLayout.NORTH);
        card.add(mid, BorderLayout.CENTER);
        return card;
    }

    void updateRecommendation(String type, int modelRow, double scoreOrMinus){
        // index baru:
        // 0 Seller, 1 Menu, 2 Price, 3 Rating, 4 Status, 5 Action, 6 ReqID(hidden)
        String seller = String.valueOf(model.getValueAt(modelRow,0));
        String menu = String.valueOf(model.getValueAt(modelRow,1));
        String price = String.valueOf(model.getValueAt(modelRow,2));
        String rating = String.valueOf(model.getValueAt(modelRow,3));
        String status = String.valueOf(model.getValueAt(modelRow,4));

        recTitle.setText(type + ": " + menu);
        recSub.setText(price + "  •  Rating " + rating + "  •  Seller " + seller);
        if(scoreOrMinus >= 0){
            recMeta.setText("Status: " + status + "  •  Score: " + String.format(Locale.US, "%.3f", scoreOrMinus));
        } else {
            recMeta.setText("Status: " + status);
        }
    }

    void refreshCatalogInfo(){
        int shown = table.getRowCount();
        int total = model.getRowCount();
        String req = (lastRequestId == -1) ? "-" : ("#" + lastRequestId);
        catalogInfoBar.setText("Request: " + req + "  •  Menus shown: " + shown + "  •  Total stored: " + total);
    }

    void hideColumn(String colName){
        TableColumn col = table.getColumn(colName);
        table.removeColumn(col);
    }

    void setColWidth(String colName, int w){
        TableColumn col = table.getColumn(colName);
        col.setMinWidth(w);
        col.setPreferredWidth(w);
        col.setMaxWidth(w + 140);
    }

    // ================= SELLERS =================
    JPanel buildSellersPanel() {
        JPanel body = new JPanel(new BorderLayout(10,10));
        body.setOpaque(false);

        sellersInfoBar = infoBar("Active sellers: " + SELLER_COUNT);

        JPanel list = new JPanel();
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setOpaque(false);

        for (SellerUI s : sellers) {
            list.add(createSellerCard(s));
            list.add(Box.createVerticalStrut(12));
        }

        JScrollPane scroll = new JScrollPane(list);
        styleScroll(scroll);

        JPanel center = new JPanel(new BorderLayout(10,10));
        center.setOpaque(false);
        center.add(sellersInfoBar, BorderLayout.NORTH);
        center.add(wrapSoft(scroll), BorderLayout.CENTER);

        body.add(center, BorderLayout.CENTER);
        return section("Sellers", body, pinkA);
    }

    void refreshSellersInfo(){
        sellersInfoBar.setText("Active sellers: " + SELLER_COUNT + "  •  Last request: " + (lastRequestId==-1? "-" : "#"+lastRequestId));
    }

    JPanel createSellerCard(SellerUI s) {
        JPanel wrap = new GlassCard(20, 0.70f);
        wrap.setLayout(new BorderLayout(10,10));
        wrap.setBorder(new EmptyBorder(12,12,12,12));
        wrap.setMaximumSize(new Dimension(Integer.MAX_VALUE, 460));

        JLabel title = new JLabel("Seller " + s.id);
        title.setFont(fontTitle);
        title.setForeground(ink);

        s.chat = new JTextArea();
        s.chat.setEditable(false);
        s.chat.setFont(fontMain);
        s.chat.setLineWrap(true);
        s.chat.setWrapStyleWord(true);
        s.chat.setRows(3);
        s.chat.setForeground(new Color(55, 65, 81));
        s.chat.setBackground(new Color(255,255,255));

        s.chatScroll = new JScrollPane(s.chat);
        s.chatScroll.setPreferredSize(new Dimension(100, 90));
        styleScroll(s.chatScroll);

        s.requestContainer = new JPanel();
        s.requestContainer.setLayout(new BoxLayout(s.requestContainer, BoxLayout.Y_AXIS));
        s.requestContainer.setOpaque(false);

        JPanel reqWrap = new JPanel(new BorderLayout());
        reqWrap.setOpaque(false);
        reqWrap.add(s.requestContainer, BorderLayout.NORTH);

        JPanel top = new JPanel(new BorderLayout(6,6));
        top.setOpaque(false);
        top.add(title, BorderLayout.NORTH);
        top.add(wrapSoft(s.chatScroll), BorderLayout.CENTER);

        wrap.add(top, BorderLayout.NORTH);
        wrap.add(wrapSoft(reqWrap), BorderLayout.CENTER);

        JPanel outer = new JPanel(new BorderLayout());
        outer.setOpaque(false);
        outer.add(wrap, BorderLayout.CENTER);
        return outer;
    }

    JPanel createRequestForSeller(String text, int reqId, SellerUI seller) {
        JPanel c = new GlassCard(18, 0.68f);
        c.setLayout(new BorderLayout(10,10));
        c.setBorder(new EmptyBorder(10,10,10,10));

        JLabel title = new JLabel("Request");
        title.setFont(fontBold);
        title.setForeground(ink);

        JLabel subtitle = new JLabel(text);
        subtitle.setFont(fontMain);
        subtitle.setForeground(muted);

        JPanel head = new JPanel(new BorderLayout(4,4));
        head.setOpaque(false);
        head.add(title, BorderLayout.NORTH);
        head.add(subtitle, BorderLayout.CENTER);

        JTextField m1=input("Menu"), p1=input("Price"), r1=input("Rating 1-5");
        JTextField m2=input("Menu"), p2=input("Price"), r2=input("Rating 1-5");
        JTextField m3=input("Menu"), p3=input("Price"), r3=input("Rating 1-5");

        JPanel grid = new JPanel(new GridLayout(3,1,8,8));
        grid.setOpaque(false);
        grid.add(row3(m1,p1,r1));
        grid.add(row3(m2,p2,r2));
        grid.add(row3(m3,p3,r3));

        FancyButton send = new FancyButton("Send", accentA, accentB);
        FancyButton done = new FancyButton("Done", purpleA, purpleB);

        JPanel btns = new JPanel(new GridLayout(1,2,10,10));
        btns.setOpaque(false);
        btns.add(send);
        btns.add(done);

        List<Integer> reqRows = new ArrayList<>();

        send.addActionListener(e -> {
            reqRows.clear();
            addMenuTrack(reqId, seller.id, m1,p1,r1, reqRows);
            addMenuTrack(reqId, seller.id, m2,p2,r2, reqRows);
            addMenuTrack(reqId, seller.id, m3,p3,r3, reqRows);

            addBuyerBubble("Seller " + seller.id + " sent menus", false);
            seller.chat.append("Sent menus\n");

            lock(m1,p1,r1,m2,p2,r2,m3,p3,r3);
            send.setEnabled(false);

            applyFilters();
            refreshCatalogInfo();
        });

        done.addActionListener(e -> {
            for(int rowIndex : reqRows){
                if(String.valueOf(model.getValueAt(rowIndex,4)).equals("Ordered")){
                    model.setValueAt("Ready", rowIndex, 4); // Status
                    model.setValueAt("Ready", rowIndex, 5); // Action
                }
            }
            addBuyerBubble("Seller " + seller.id + ": order is READY", false);
            seller.chat.append("Order is READY\n");
            done.setEnabled(false);

            applyFilters();
            refreshCatalogInfo();
        });

        c.add(head, BorderLayout.NORTH);
        c.add(grid, BorderLayout.CENTER);
        c.add(btns, BorderLayout.SOUTH);

        return c;
    }

    void lock(JTextField... fields){
        for(JTextField f: fields) f.setEnabled(false);
    }

    // ================= FILTERING =================
    void applyFilters(){
        if(sorter == null) return;

        final String q = (searchField == null) ? "" : searchField.getText().trim().toLowerCase(Locale.ROOT);
        final boolean onlyAvail = (onlyAvailableCb != null) && onlyAvailableCb.isSelected();
        final boolean thisReqOnly = (thisRequestOnlyCb != null) && thisRequestOnlyCb.isSelected();
        final double minRating = getMinRating();

        sorter.setRowFilter(new RowFilter<DefaultTableModel, Integer>() {
            @Override
            public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> entry) {

                // index baru:
                // 0 Seller, 1 Menu, 2 Price, 3 Rating, 4 Status, 5 Action, 6 ReqID(hidden)
                int reqId = toInt(entry.getStringValue(6), -1);
                String menu = safe(entry.getStringValue(1)).toLowerCase(Locale.ROOT);
                String status = safe(entry.getStringValue(4));
                double rating = toDouble(safe(entry.getStringValue(3)), 0);

                if(thisReqOnly && lastRequestId != -1 && reqId != lastRequestId) return false;
                if(onlyAvail && !status.equalsIgnoreCase("Available")) return false;
                if(rating < minRating) return false;
                if(!q.isEmpty() && !menu.contains(q)) return false;

                return true;
            }
        });

        refreshCatalogInfo();
    }

    double getMinRating(){
        if(minRatingCb == null) return 0;
        String s = String.valueOf(minRatingCb.getSelectedItem());
        if(s == null || s.equals("Any")) return 0;
        try{
            s = s.replace("+","").trim();
            return Double.parseDouble(s);
        } catch(Exception e){
            return 0;
        }
    }

    // ================= AI =================
    void showAIBestCurrentReq() {
        if (lastRequestId == -1) {
            addBuyerBubble("AI: send a request first", false);
            return;
        }

        List<Integer> rows = new ArrayList<>();
        int minPrice = Integer.MAX_VALUE;
        int maxPrice = Integer.MIN_VALUE;

        for(int r=0; r<model.getRowCount(); r++){
            int reqId = toInt(String.valueOf(model.getValueAt(r,6)), -1);
            if(reqId != lastRequestId) continue;

            rows.add(r);
            int price = parsePrice(String.valueOf(model.getValueAt(r,2)));
            minPrice = Math.min(minPrice, price);
            maxPrice = Math.max(maxPrice, price);
        }

        if(rows.isEmpty()){
            addBuyerBubble("AI: no menu yet for this request", false);
            return;
        }

        int priceRange = Math.max(1, maxPrice - minPrice);

        int bestRow = -1;
        double bestScore = -1;

        for(int r : rows){
            double rating = parseRating(String.valueOf(model.getValueAt(r,3)));
            int price = parsePrice(String.valueOf(model.getValueAt(r,2)));

            double ratingNorm = clamp01(rating / 5.0);
            double cheapNorm = 1.0 - ((price - minPrice) / (double) priceRange);
            cheapNorm = clamp01(cheapNorm);

            double score = (W_RATING * ratingNorm) + (W_PRICE * cheapNorm);

            if(score > bestScore){
                bestScore = score;
                bestRow = r;
            }
        }

        if(bestRow == -1) return;

        thisRequestOnlyCb.setSelected(true);
        onlyAvailableCb.setSelected(false);
        applyFilters();

        selectAndScrollToModelRow(bestRow);
        addBuyerBubble("AI Best: " + model.getValueAt(bestRow,1) + " • " + model.getValueAt(bestRow,2)
                + " • Rating " + model.getValueAt(bestRow,3) + " • Seller " + model.getValueAt(bestRow,0), false);

        updateRecommendation("AI Best", bestRow, bestScore);
    }

    void showCheapestCurrentReq() {
        if (lastRequestId == -1) {
            addBuyerBubble("Cheapest: send a request first", false);
            return;
        }

        int bestRow = -1;
        int bestPrice = Integer.MAX_VALUE;

        for(int r=0; r<model.getRowCount(); r++){
            int reqId = toInt(String.valueOf(model.getValueAt(r,6)), -1);
            if(reqId != lastRequestId) continue;

            int price = parsePrice(String.valueOf(model.getValueAt(r,2)));
            if(price < bestPrice){
                bestPrice = price;
                bestRow = r;
            }
        }

        if(bestRow == -1){
            addBuyerBubble("Cheapest: no menu yet for this request", false);
            return;
        }

        thisRequestOnlyCb.setSelected(true);
        applyFilters();

        selectAndScrollToModelRow(bestRow);
        addBuyerBubble("Cheapest: " + model.getValueAt(bestRow,1) + " (" + model.getValueAt(bestRow,2) + ")", false);

        updateRecommendation("Cheapest", bestRow, -1);
    }

    void selectAndScrollToModelRow(int modelRow){
        int viewRow = table.convertRowIndexToView(modelRow);
        if(viewRow < 0) return;
        table.setRowSelectionInterval(viewRow, viewRow);
        Rectangle rect = table.getCellRect(viewRow, 0, true);
        table.scrollRectToVisible(rect);
    }

    // ================= TABLE BUTTONS =================
    class BtnRender extends JButton implements TableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c){
            String label = (v == null || String.valueOf(v).trim().isEmpty()) ? "Order" : String.valueOf(v);
            FancyButton b = new FancyButton(label, pinkA, pinkB);
            b.setFont(fontBold);
            b.setPreferredSize(new Dimension(94, 30));
            return b;
        }
    }

    class BtnEditor extends DefaultCellEditor {
        JButton b;
        int viewRow;

        BtnEditor() {
            super(new JTextField());
            b = new FancyButton("", pinkA, pinkB);
            b.addActionListener(e -> click());
        }

        @Override
        public Component getTableCellEditorComponent(JTable t,Object v,boolean s,int row,int c){
            viewRow = row;
            String label = (v == null || String.valueOf(v).trim().isEmpty()) ? "Order" : String.valueOf(v);
            b.setText(label);
            return b;
        }

        void click() {
            int modelRow = table.convertRowIndexToModel(viewRow);
            if(modelRow < 0) return;

            String status = String.valueOf(model.getValueAt(modelRow,4));
            if(status.equalsIgnoreCase("Available")){
                model.setValueAt("Ordered", modelRow, 4); // Status
                model.setValueAt("Ordered", modelRow, 5); // Action

                String menu = String.valueOf(model.getValueAt(modelRow,1));
                String sellerId = String.valueOf(model.getValueAt(modelRow,0));

                addBuyerBubble("Ordered: " + menu + " (Seller " + sellerId + ")", false);

                int sid = toInt(sellerId, -1);
                for(SellerUI s : sellers){
                    if(s.id == sid){
                        s.chat.append("New order: " + menu + "\n");
                        break;
                    }
                }

                applyFilters();
                refreshCatalogInfo();
            }
            fireEditingStopped();
        }
    }

    // ================= INVOICE =================
    void showInvoice(int modelRow) {
        JTextArea a = new JTextArea(
                "INVOICE\n----------------\n" +
                        "Request : #" + model.getValueAt(modelRow,6) +
                        "\nSeller  : Seller " + model.getValueAt(modelRow,0) +
                        "\nItem    : " + model.getValueAt(modelRow,1) +
                        "\nPrice   : " + model.getValueAt(modelRow,2) +
                        "\nRating  : " + model.getValueAt(modelRow,3) +
                        "\nStatus  : PAID\n----------------"
        );
        a.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JOptionPane.showMessageDialog(this, new JScrollPane(a), "Invoice", JOptionPane.INFORMATION_MESSAGE);
    }

    // ================= HELPERS =================
    void addMenuTrack(int reqId, int sellerId, JTextField m, JTextField p, JTextField ratingField, List<Integer> rows){
        if(m.getText().startsWith("Menu") || p.getText().startsWith("Price") || ratingField.getText().startsWith("Rating")) return;

        int price;
        try { price = Integer.parseInt(p.getText().replaceAll("\\D","")); }
        catch(Exception ex){ return; }

        double rating = parseRating(ratingField.getText());
        if(rating <= 0) rating = 3.0;

        // ✅ FIX: jumlah kolom harus PAS (7 kolom)
        model.addRow(new Object[]{
                sellerId,                // Seller (paling kiri) -> 1..6
                m.getText(),             // Menu
                rupiah.format(price),    // Price
                String.valueOf(rating),  // Rating
                "Available",             // Status
                "Order",                 // Action (biar ga null)
                reqId                    // ReqID (hidden)
        });
        rows.add(model.getRowCount()-1);
    }

    int parsePrice(String formatted){
        return Integer.parseInt(formatted.replaceAll("\\D",""));
    }

    double parseRating(String text){
        String cleaned = text.replaceAll("[^0-9.]", "");
        if(cleaned.isEmpty()) return 0;
        try{
            double v = Double.parseDouble(cleaned);
            return Math.max(0, Math.min(5, v));
        } catch(Exception e){
            return 0;
        }
    }

    double clamp01(double v){
        return Math.max(0, Math.min(1, v));
    }

    JTextField input(String ph){
        JTextField f = new JTextField(ph);
        f.setFont(fontMain);
        f.setForeground(new Color(107, 114, 128));
        f.setBorder(new CompoundBorder(new LineBorder(new Color(0,0,0,20), 1, true), new EmptyBorder(9,10,9,10)));
        f.setBackground(Color.WHITE);
        f.addFocusListener(new FocusAdapter(){
            public void focusGained(FocusEvent e){
                if(f.getText().equals(ph)){ f.setText(""); f.setForeground(new Color(17, 24, 39)); }
            }
            public void focusLost(FocusEvent e){
                if(f.getText().isEmpty()){ f.setText(ph); f.setForeground(new Color(107, 114, 128)); }
            }
        });
        return f;
    }

    JPanel row3(JTextField a, JTextField b, JTextField c){
        JPanel p=new JPanel(new GridLayout(1,3,8,8));
        p.setOpaque(false);
        p.add(a);p.add(b);p.add(c);
        return p;
    }

    JLabel infoBar(String text){
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        l.setForeground(new Color(55, 65, 81));
        l.setBorder(new CompoundBorder(
                new LineBorder(new Color(0,0,0,14), 1, true),
                new EmptyBorder(8,10,8,10)
        ));
        l.setOpaque(true);
        l.setBackground(new Color(255,255,255,200));
        return l;
    }

    JPanel section(String titleText, JComponent body, Color accent){
        JPanel outer = new GlassCard(24, 0.72f);
        outer.setLayout(new BorderLayout(10,10));
        outer.setBorder(new EmptyBorder(12,12,12,12));

        JLabel title = new JLabel(titleText);
        title.setFont(fontTitle);
        title.setForeground(ink);

        JPanel underline = new JPanel(){
            protected void paintComponent(Graphics g){
                super.paintComponent(g);
                Graphics2D g2=(Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 140));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 999, 999);
                g2.dispose();
            }
        };
        underline.setOpaque(false);
        underline.setPreferredSize(new Dimension(40, 6));

        JPanel head = new JPanel(new BorderLayout(8,0));
        head.setOpaque(false);
        head.add(title, BorderLayout.WEST);
        head.add(underline, BorderLayout.SOUTH);

        outer.add(head, BorderLayout.NORTH);
        outer.add(body, BorderLayout.CENTER);

        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.add(outer, BorderLayout.CENTER);
        return wrap;
    }

    JComponent wrapSoft(JComponent inner){
        JPanel p = new GlassCard(20, 0.68f);
        p.setLayout(new BorderLayout());
        p.setBorder(new EmptyBorder(10,10,10,10));
        p.add(inner, BorderLayout.CENTER);
        return p;
    }

    void styleScroll(JScrollPane sp){
        sp.setWheelScrollingEnabled(true);
        sp.setBorder(new EmptyBorder(0,0,0,0));
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        sp.getVerticalScrollBar().setUnitIncrement(18);
        sp.setFocusable(false);
        sp.getVerticalScrollBar().setPreferredSize(new Dimension(12, 0));
    }

    // ================= RENDERERS =================
    static class ZebraCellRenderer extends DefaultTableCellRenderer {
        private final Color odd, even, ink, sel;
        ZebraCellRenderer(Color odd, Color even, Color ink, Color sel){
            this.odd = odd; this.even = even; this.ink = ink; this.sel = sel;
            setBorder(new EmptyBorder(6,10,6,10));
        }
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            JLabel c = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            c.setForeground(ink);
            c.setOpaque(true);
            c.setBackground(isSelected ? sel : ((row % 2 == 0) ? even : odd));
            return c;
        }
    }

    static class GradientHeaderRenderer extends DefaultTableCellRenderer {
        private final Color a, b, text;
        GradientHeaderRenderer(Color a, Color b, Color text){
            this.a=a; this.b=b; this.text=text;
            setHorizontalAlignment(CENTER);
        }
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            l.setForeground(text);
            l.setFont(new Font("Segoe UI", Font.BOLD, 13));
            l.setBorder(new EmptyBorder(6,10,6,10));
            return new HeaderPaintPanel(l, a, b);
        }
        static class HeaderPaintPanel extends JPanel {
            private final JLabel label;
            private final Color a, b;
            HeaderPaintPanel(JLabel label, Color a, Color b){
                this.label=label; this.a=a; this.b=b;
                setLayout(new BorderLayout());
                add(label, BorderLayout.CENTER);
            }
            @Override protected void paintComponent(Graphics g){
                Graphics2D g2=(Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setPaint(new GradientPaint(0,0,a, getWidth(),getHeight(),b));
                g2.fillRect(0,0,getWidth(),getHeight());
                g2.dispose();
                super.paintComponent(g);
            }
        }
    }

    // ================= UI COMPONENTS =================
    static class GlassCard extends JPanel {
        private final int radius;
        private final float alpha;
        GlassCard(int radius, float alpha){
            this.radius = radius;
            this.alpha = alpha;
            setOpaque(false);
        }
        @Override protected void paintComponent(Graphics g){
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth(), h = getHeight();

            g2.setColor(new Color(0,0,0,12));
            g2.fillRoundRect(2, 3, w-4, h-4, radius, radius);

            g2.setComposite(AlphaComposite.SrcOver.derive(alpha));
            g2.setColor(Color.WHITE);
            g2.fillRoundRect(0, 0, w-4, h-4, radius, radius);

            g2.setComposite(AlphaComposite.SrcOver);
            g2.setColor(new Color(0,0,0,14));
            g2.drawRoundRect(0, 0, w-5, h-5, radius, radius);

            g2.dispose();
            super.paintComponent(g);
        }
    }

    static class CalmBackground extends JPanel {
        private final Color top, bottom;
        CalmBackground(Color top, Color bottom){
            this.top = top; this.bottom = bottom;
            setOpaque(true);
        }
        @Override protected void paintComponent(Graphics g){
            super.paintComponent(g);
            Graphics2D g2=(Graphics2D)g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setPaint(new GradientPaint(0,0, top, 0,getHeight(), bottom));
            g2.fillRect(0,0,getWidth(),getHeight());
            g2.dispose();
        }
    }

    static class FancyButton extends JButton {
        private final Color a, b;
        FancyButton(String text, Color a, Color b){
            super(text);
            this.a = a; this.b = b;
            setFocusPainted(false);
            setBorder(new EmptyBorder(10,14,10,14));
            setForeground(Color.WHITE);
            setFont(new Font("Segoe UI", Font.BOLD, 13));
            setContentAreaFilled(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }
        @Override protected void paintComponent(Graphics g){
            Graphics2D g2=(Graphics2D)g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w=getWidth(), h=getHeight();
            Color a2=a, b2=b;
            if(!isEnabled()){
                a2 = new Color(156, 163, 175);
                b2 = new Color(209, 213, 219);
            } else if(getModel().isPressed()){
                a2 = a.darker();
                b2 = b.darker();
            }

            g2.setPaint(new GradientPaint(0,0,a2, w,h,b2));
            g2.fillRoundRect(0,0,w,h,16,16);

            g2.setColor(new Color(0,0,0,18));
            g2.drawRoundRect(0,0,w-1,h-1,16,16);

            g2.dispose();
            super.paintComponent(g);
        }
    }

    // ================= small utils =================
    static String safe(String s){ return s == null ? "" : s; }
    static int toInt(String s, int def){
        try { return Integer.parseInt(s.replaceAll("\\D","")); } catch(Exception e){ return def; }
    }
    static double toDouble(String s, double def){
        try { return Double.parseDouble(s.replaceAll("[^0-9.]", "")); } catch(Exception e){ return def; }
    }

    // ================= MAIN =================
    public static void main(String[] args) {
        SwingUtilities.invokeLater(FoodOrderApp::new);
    }
}