package com.example.lifeguard.Api;

import java.util.Map;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class Categories {
        private Map<String, Boolean> categories;
        private Map<String, Double> category_scores;
        boolean flagged;

        public String getSelfHarmCategory(){
                return String.valueOf(categories.get("self-harm"));
        }
        public String getSelfHarmCategoryScore(){
                return String.valueOf(category_scores.get("self-harm"));
        }
}
