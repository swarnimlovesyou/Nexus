package com.nexus.service;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.nexus.domain.Memory;

public class ExportService {
    
    /**
     * Exports the logged-in user's memory vault to a CSV file.
     * Demonstrates File I/O for the assignment context.
     * 
     * @param memories The list of memories to export
     * @param filename Target filename
     * @return Path to the created file
     */
    public String exportToCsv(List<Memory> memories, String filename) throws IOException {
        String filepath = System.getProperty("user.dir") + System.getProperty("file.separator") + filename;
        
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filepath))) {
            // Write CSV Header
            writer.write("ID,Scope,Type,Pinned,Confidence,TTL_Days,Created_At,Tags,Content\n");
            
            DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
            
            for (Memory m : memories) {
                // Escape quotes in content
                String safeContent = m.getContent().replace("\"", "\"\"");
                String safeTags = m.getTags() != null ? m.getTags().replace("\"", "\"\"") : "";
                String scope = m.getAgentId() != null ? m.getAgentId() : "global";
                boolean pinned = m.getTags() != null && m.getTags().toLowerCase().contains("pinned:true");
                
                String line = String.format("%d,%s,%s,%s,%.4f,%d,%s,\"%s\",\"%s\"\n",
                    m.getId(),
                    scope,
                    m.getType().name(),
                    pinned ? "yes" : "no",
                    m.getConfidence(),
                    m.daysUntilExpiry() == Long.MAX_VALUE ? 9999 : m.daysUntilExpiry(),
                    m.getCreatedAt().format(formatter),
                    safeTags,
                    safeContent
                );
                writer.write(line);
            }
        }
        
        return filepath;
    }
}
