package com.example.studentmanagementapp;

public class ExamType {
    private String name;
    private double coefficient;

    public ExamType(String name, double coefficient) {
        this.name = name;
        this.coefficient = coefficient;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getCoefficient() { return coefficient; }
    public void setCoefficient(double coefficient) { this.coefficient = coefficient; }
}