package com.checkstylehub.analyzer.dto;

/**
 * DTO for direct code analysis request.
 * Allows users to submit Java code directly without a GitHub repository.
 */
public class CodeAnalysisRequestDto {
    
    /** The Java source code to analyze */
    private String code;
    
    /** Optional filename (defaults to "Main.java" if not provided) */
    private String fileName;
    
    /** Optional custom Checkstyle XML configuration */
    private String checkstyleConfig;
    
    /** Whether to check if the code compiles */
    private boolean checkCompilation = true;
    
    public CodeAnalysisRequestDto() {}
    
    public CodeAnalysisRequestDto(String code) {
        this.code = code;
    }
    
    public String getCode() {
        return code;
    }
    
    public void setCode(String code) {
        this.code = code;
    }
    
    public String getFileName() {
        return fileName;
    }
    
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
    
    public String getCheckstyleConfig() {
        return checkstyleConfig;
    }
    
    public void setCheckstyleConfig(String checkstyleConfig) {
        this.checkstyleConfig = checkstyleConfig;
    }
    
    public boolean isCheckCompilation() {
        return checkCompilation;
    }
    
    public void setCheckCompilation(boolean checkCompilation) {
        this.checkCompilation = checkCompilation;
    }
}
