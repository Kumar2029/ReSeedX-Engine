package com.sba.controller;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.itextpdf.text.pdf.draw.LineSeparator;
import com.sba.model.CloudFile;
import com.sba.model.Client;
import com.sba.repository.ClientRepository;
import com.sba.service.SBAService;
import com.sba.util.SBAUtils;
import com.sba.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/report")
public class ReportController {

    @Autowired private SBAService sbaService;
    @Autowired private ClientRepository clientRepo;
    @Autowired private JwtUtil jwtUtil;

    @GetMapping("/generate")
    public ResponseEntity<?> generate(HttpServletRequest req) {
        try {
            String clientId = (String) req.getAttribute("clientId");
            // Fallback: parse JWT directly from Authorization header
            if (clientId == null) {
                String auth = req.getHeader("Authorization");
                if (auth != null && auth.startsWith("Bearer ")) {
                    String token = auth.substring(7);
                    if (jwtUtil.isValid(token)) clientId = jwtUtil.extractClientId(token);
                }
            }
            // Fallback 2: query param token
            if (clientId == null) {
                String qToken = req.getParameter("token");
                if (qToken != null && !qToken.equals("null") && jwtUtil.isValid(qToken))
                    clientId = jwtUtil.extractClientId(qToken);
            }
            if (clientId == null) return ResponseEntity.status(401).body("Not authenticated");
            Client client = clientRepo.findById(clientId).orElseThrow(() -> new RuntimeException("Not found"));
            Map<String, Object> stats = sbaService.getSystemStats();
            List<CloudFile> files = sbaService.getClientFiles(client);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Document doc = new Document(PageSize.A4, 50, 50, 60, 60);
            PdfWriter.getInstance(doc, baos);
            doc.open();

            BaseColor darkBlue = new BaseColor(29, 78, 216), teal = new BaseColor(15, 118, 110),
                lightGray = new BaseColor(241, 245, 249), darkText = new BaseColor(15, 23, 42);
            Font titleF = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, darkBlue);
            Font subF = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, darkText);
            Font bodyF = FontFactory.getFont(FontFactory.HELVETICA, 10, darkText);
            Font boldF = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, darkText);
            Font smallF = FontFactory.getFont(FontFactory.HELVETICA, 8, new BaseColor(100, 116, 139));
            Font whiteF = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, BaseColor.WHITE);

            // Header
            PdfPTable hdr = new PdfPTable(1); hdr.setWidthPercentage(100);
            PdfPCell hc = new PdfPCell(); hc.setBackgroundColor(darkBlue); hc.setPadding(20); hc.setBorder(Rectangle.NO_BORDER);
            Paragraph t = new Paragraph("RESEEDX ENGINE — PHASE 4", titleF); t.setAlignment(Element.ALIGN_CENTER);
            Paragraph s = new Paragraph("Blockchain-Verified Distributed Recovery: Research Analysis Report", FontFactory.getFont(FontFactory.HELVETICA, 11, BaseColor.WHITE)); s.setAlignment(Element.ALIGN_CENTER);
            Paragraph g = new Paragraph("Generated: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")) + " | User: " + client.getUsername(), smallF); g.setAlignment(Element.ALIGN_CENTER);
            hc.addElement(t); hc.addElement(s); hc.addElement(g); hdr.addCell(hc); doc.add(hdr); doc.add(Chunk.NEWLINE);

            // Abstract
            doc.add(new Paragraph("ABSTRACT", subF));
            doc.add(new Chunk(new LineSeparator(1, 100, teal, Element.ALIGN_LEFT, -2))); doc.add(Chunk.NEWLINE);
            Paragraph abs = new Paragraph("Phase 4 of the Seed Block Algorithm introduces blockchain-verified distributed recovery with AES-256-GCM hybrid encryption combined with XOR-based Seed Block backup. " +
                "The system employs multi-threaded processing for large files, JWT stateless authentication, SHA-256 integrity verification, " +
                "GZIP compression, deduplication, triple-node backup with Triple XOR, blockchain audit trail, and automated load testing across file sizes from 1KB to 10MB.", bodyF);
            abs.setLeading(16); doc.add(abs); doc.add(Chunk.NEWLINE);

            // Stats table
            doc.add(new Paragraph("TABLE I — SYSTEM STATISTICS", subF));
            doc.add(new Chunk(new LineSeparator(1, 100, teal, Element.ALIGN_LEFT, -2))); doc.add(Chunk.NEWLINE);
            PdfPTable st = new PdfPTable(2); st.setWidthPercentage(65); st.setHorizontalAlignment(Element.ALIGN_LEFT);
            long totalOrig = files.stream().mapToLong(CloudFile::getFileSize).sum();
            String[][] srows = {
                {"Total Files", String.valueOf(files.size())},
                {"Total Storage", SBAUtils.formatFileSize(totalOrig)},
                {"Space Saved (Compression)", stats.get("spaceSaved").toString()},
                {"Duplicates Prevented", stats.get("duplicatesSaved").toString()},
                {"Avg XOR Time", stats.get("avgXorTimeMs").toString() + " ms"},
                {"Encryption", "AES-256-GCM + XOR Seed Block"},
                {"Authentication", "JWT (stateless, 24h expiry)"},
                {"Backup Servers", "3 (Primary AES+XOR | Secondary DoubleXOR | Tertiary TripleXOR)"},
                {"Integrity", "SHA-256 Hash Verification"},
                {"Threading", "Multi-threaded XOR (4 threads, 1MB chunks)"}
            };
            for (String[] row : srows) {
                PdfPCell k = new PdfPCell(new Phrase(row[0], boldF)); k.setBackgroundColor(lightGray); k.setPadding(6); k.setBorderColor(new BaseColor(226, 232, 240));
                PdfPCell v = new PdfPCell(new Phrase(row[1], bodyF)); v.setPadding(6); v.setBorderColor(new BaseColor(226, 232, 240));
                st.addCell(k); st.addCell(v);
            }
            doc.add(st); doc.add(Chunk.NEWLINE);

            // Performance table
            doc.add(new Paragraph("TABLE II — FILE PERFORMANCE ANALYSIS", subF));
            doc.add(new Chunk(new LineSeparator(1, 100, teal, Element.ALIGN_LEFT, -2))); doc.add(Chunk.NEWLINE);
            PdfPTable pt = new PdfPTable(6); pt.setWidthPercentage(100); pt.setWidths(new float[]{3f,1.4f,1.4f,1.2f,1.4f,1.2f});
            for (String h : new String[]{"File Name","Original Size","Compressed","Ratio","XOR+AES (ms)","Compressed?"}) {
                PdfPCell c = new PdfPCell(new Phrase(h, whiteF)); c.setBackgroundColor(darkBlue); c.setPadding(7); c.setBorder(Rectangle.NO_BORDER); c.setHorizontalAlignment(Element.ALIGN_CENTER); pt.addCell(c);
            }
            boolean alt = false;
            for (CloudFile f : files) {
                BaseColor bg = alt ? lightGray : BaseColor.WHITE;
                String cs = f.getCompressedSize() > 0 ? SBAUtils.formatFileSize(f.getCompressedSize()) : SBAUtils.formatFileSize(f.getFileSize());
                for (String val : new String[]{f.getFileName()!=null?f.getFileName():"unknown", SBAUtils.formatFileSize(f.getFileSize()), cs, f.getCompressionRatio()+"x", f.getUploadTimeMs()+" ms", f.isCompressed()?"Yes":"No"}) {
                    PdfPCell c = new PdfPCell(new Phrase(val, bodyF)); c.setBackgroundColor(bg); c.setPadding(6); c.setBorderColor(new BaseColor(226,232,240)); c.setHorizontalAlignment(Element.ALIGN_CENTER); pt.addCell(c);
                }
                alt = !alt;
            }
            doc.add(pt); doc.add(Chunk.NEWLINE);

            // Algorithm
            doc.add(new Paragraph("TABLE III — PHASE 4 ALGORITHM STEPS", subF));
            doc.add(new Chunk(new LineSeparator(1, 100, teal, Element.ALIGN_LEFT, -2))); doc.add(Chunk.NEWLINE);
            PdfPTable at = new PdfPTable(2); at.setWidthPercentage(100); at.setWidths(new float[]{2.5f,4f});
            String[][] algoRows = {
                {"Step 1 — Registration","Seed = ClientID ⊕ RandomNumber(256-bit)"},
                {"Step 2 — Compression","Compressed = GZIP(File)  [if saving > 5%]"},
                {"Step 3 — Multi-thread XOR","File' = XOR_MT(Compressed, Seed)  [4 threads, 1MB chunks]"},
                {"Step 4 — AES-256-GCM","Encrypted = AES-256(File')  → Node A (Primary)"},
                {"Step 5 — Secondary Backup","File'' = DoubleXOR(Compressed, Seed, ClientID)  → Node B"},
                {"Step 6 — Tertiary Backup","File''' = TripleXOR(Compressed, Seed, ClientID, Rev(Seed))  → Node C"},
                {"Step 7 — Blockchain Audit","Block = SHA-256(Index|PrevHash|Action|Nonce|Timestamp)"},
                {"Step 8 — Integrity","SHA-256(Original) stored as checksum"},
                {"Step 9 — Recovery","AES-Decrypt → XOR → Decompress → Verify SHA-256 (auto-failover)"}
            };
            for (String[] row : algoRows) {
                PdfPCell k = new PdfPCell(new Phrase(row[0], boldF)); k.setBackgroundColor(lightGray); k.setPadding(7); k.setBorderColor(new BaseColor(226,232,240));
                PdfPCell v = new PdfPCell(new Phrase(row[1], FontFactory.getFont(FontFactory.COURIER, 9, darkText))); v.setPadding(7); v.setBorderColor(new BaseColor(226,232,240));
                at.addCell(k); at.addCell(v);
            }
            doc.add(at); doc.add(Chunk.NEWLINE);

            // Footer
            doc.add(new Chunk(new LineSeparator(0.5f, 100, new BaseColor(226,232,240), Element.ALIGN_CENTER, -2)));
            Paragraph footer = new Paragraph("ReSeedX Engine Phase 4 | " + client.getUsername() + " | " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy")),
                FontFactory.getFont(FontFactory.HELVETICA, 8, new BaseColor(148,163,184)));
            footer.setAlignment(Element.ALIGN_CENTER); doc.add(footer);
            doc.close();

            String fn = "ReSeedX_Engine_Report_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm")) + ".pdf";
            return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fn + "\"")
                .contentType(MediaType.APPLICATION_PDF).body(baos.toByteArray());
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Report failed: " + e.getMessage()));
        }
    }
}
