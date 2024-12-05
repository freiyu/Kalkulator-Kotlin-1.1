package com.example.kotlincalculator

class MathUtils {
  companion object {
    fun evaluateExpression(expression: List<String>): Double {
      val processedExpression = combineNumberTokens(expression)

      val outputQueue = mutableListOf<String>()
      val operatorStack = mutableListOf<String>()

      for (token in processedExpression) {
        when {
          token.toDoubleOrNull() != null -> outputQueue.add(token)

          isOperator(token) -> {
            while (operatorStack.isNotEmpty() &&
              hasHigherPrecedence(operatorStack.last(), token)) {
              outputQueue.add(operatorStack.removeAt(operatorStack.lastIndex))
            }
            operatorStack.add(token)
          }
        }
      }

      while (operatorStack.isNotEmpty()) {
        outputQueue.add(operatorStack.removeAt(operatorStack.lastIndex))
      }

      val stack = mutableListOf<Double>()
      for (token in outputQueue) {
        when {
          token.toDoubleOrNull() != null -> stack.add(token.toDouble())
          isOperator(token) -> {
            if (stack.size < 2) throw IllegalArgumentException("Invalid expression")
            val b = stack.removeAt(stack.lastIndex)
            val a = stack.removeAt(stack.lastIndex)
            stack.add(performOperation(a, b, token))
          }
        }
      }

      return stack.last()
    }

    private fun combineNumberTokens(expression: List<String>): List<String> {
      val processedExpression = mutableListOf<String>()
      var currentNumber = ""
      var lastToken: String? = null

      for (token in expression) {
        when {
          // Check if token is a number or decimal point
          token.matches(Regex("[0-9.]")) -> {
            // If last token was an operator or first token, start a new number
            if (lastToken == null || isOperator(lastToken!!) || lastToken == "(") {
              if (currentNumber.isNotEmpty()) {
                processedExpression.add(currentNumber)
                currentNumber = ""
              }
            }

            // Prevent multiple decimal points
            if (token == "." && currentNumber.contains(".")) {
              continue
            }

            currentNumber += token
          }
          // Non-number token
          else -> {
            // Add the accumulated number if exists
            if (currentNumber.isNotEmpty()) {
              processedExpression.add(currentNumber)
              currentNumber = ""
            }

            // Add the current token
            processedExpression.add(token)
          }
        }

        // Update last token
        lastToken = token
      }

      // Add final number if exists
      if (currentNumber.isNotEmpty()) {
        processedExpression.add(currentNumber)
      }

      return processedExpression
    }

    private fun hasHigherPrecedence(op1: String, op2: String): Boolean {
      return when {
        (op1 == "*" || op1 == "/") && (op2 == "+" || op2 == "-") -> true
        else -> false
      }
    }

    private fun isOperator(token: String): Boolean {
      return token in listOf("+", "-", "x", "/")
    }

    private fun performOperation(a: Double, b: Double, operator: String): Double {
      return when (operator) {
        "+" -> a + b
        "-" -> a - b
        "x" -> a * b
        "/" -> {
          if (b == 0.0) throw ArithmeticException("Division by zero")
          a / b
        }
        else -> throw IllegalArgumentException("Invalid operator")
      }
    }
  }
}