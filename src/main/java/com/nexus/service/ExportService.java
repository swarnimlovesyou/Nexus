package com.nexus.service;

import com.nexus.domain.Memory;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

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
            writer.write("ID,Type,Confidence,TTL_Days,Created_At,Tags,Content\n");
            
            DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
            
            for (Memory m : memories) {
                // Escape quotes in content
                String safeContent = m.getContent().replace("\"", "\"\"");
                String safeTags = m.getTags() != null ? m.getTags().replace("\"", "\"\"") : "";
                
                String line = String.format("%d,%s,%.4f,%d,%s,\"%s\",\"%s\"\n",
                    m.getId(),
                    m.getType().name(),
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
