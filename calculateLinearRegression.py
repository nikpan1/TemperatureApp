def calculate_line_equation(point1, point2):
    x1, y1 = point1
    x2, y2 = point2

    # Calculate the slope (a)
    a = (y2 - y1) / (x2 - x1)

    # Calculate the y-intercept (b)
    b = y1 - a * x1

    return a, b

# Given data points
point1 = (2.5180151870910 * 0.0001, 26)
point2 = (2.4103567188033 * 0.0001, 65)

# Calculate the slope and y-intercept
a, b = calculate_line_equation(point1, point2)

# Output the equation of the line
print(f"The equation of the line is: y = {a}x + {b}")

# Verify the calculation
print(f"Slope (a): {a}")
print(f"Y-intercept (b): {b}")

# Optional: check the line equation with the given points
y1_calculated = a * point1[0] + b
y2_calculated = a * point2[0] + b
print(f"Verification with point 1: y = {y1_calculated}, expected y = {point1[1]}")
print(f"Verification with point 2: y = {y2_calculated}, expected y = {point2[1]}")

