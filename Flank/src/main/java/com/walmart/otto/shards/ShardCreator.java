package com.walmart.otto.shards;

import com.walmart.otto.configurator.Configurator;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

class ShardCreator {
  private Configurator configurator;

  ShardCreator(Configurator configurator) {
    this.configurator = configurator;
  }

  List<String> getShards(List<String> testCases) {
    int testsInShard = configurator.getTestsInShard();

    Stack<String> availableTestCases = new Stack<>();
    availableTestCases.addAll(testCases);

    List<String> shards = new ArrayList<>();

    while (!availableTestCases.isEmpty()) {
      String test = addToShard(availableTestCases, testsInShard);

      if (!test.isEmpty()) {
        shards.add(test);
      }
    }

    return shards;
  }

  private String addToShard(Stack<String> stack, int numTestsInShards) {
    StringBuilder stringBuilder = new StringBuilder();

    if (stack.isEmpty()) {
      return "";
    }

    for (int i = 0; i < numTestsInShards && !stack.isEmpty(); i++) {
      addTestToBuilder(stack.pop(), stringBuilder);
    }

    return stringBuilder.toString();
  }

  private void addTestToBuilder(String stringToAdd, StringBuilder stringBuilder) {
    stringBuilder.append("class ").append(stringToAdd);
    stringBuilder.append(",");
  }
}
