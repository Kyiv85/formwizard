import java.io.*;

public class Persona{
	//La variable de instancia nombre puede ser vista por todos los hijos de la clase
	public String nombre;

	//Peso es una variable solo vista por la clase persona
	private double peso;

	//La variable nombre es asignada en el constructor
	public Persona(String nombre){
		this.nombre = nombre;
	}

	//Este método asigna un peso a la variable peso
	public void setPeso(double peso){
		this.peso = peso;
	}

	//Este método imprime los datos de la persona
	public void imprimirPersona(){
		System.out.println("Nombre: "+this.nombre);
		System.out.println("Peso: "+this.peso);
	}

	public static void main(String[] args) {
		Persona alguien = new Persona("Carlos");
		alguien.setPeso(80);
		alguien.imprimirPersona();	
	}
}