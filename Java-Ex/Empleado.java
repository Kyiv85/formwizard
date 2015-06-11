import java.io.*;

public class Empleado{
	//Salario es una variable estática privada de la clase empleado
	private static double salario;

	//Departamento es una constante
	public static final String departamento = "Desarrollo";

	public static void main(String args[]){
		salario = 2000;
		System.out.println("El departamento "+departamento+" tiene un salario promedio de "+salario);
	}
}