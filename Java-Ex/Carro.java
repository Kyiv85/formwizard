public class Carro{
	//String marca;
	//int kilometraje;
	//String color;

/*	public Carro(String marca){
		//El constructor tiene solo un parámetro, en este caso marca
		System.out.println("La marca es: "+marca);
	}

	public static void main(String []args){
		//Creamos la variable carro
		Carro miCarro = new Carro("Toyota");
	}
}
*/

	int kilometraje;

	public Carro(String marca){
		// El constructor tiene solo un parametro, en este caso marca
		System.out.println("La marca es : "+marca);		
	}

	public void setKilometraje(int kilometraje){
		this.kilometraje = kilometraje;
	}

	public int getKilometraje(){
		System.out.println("El kilometraje es : "+kilometraje);
		return this.kilometraje;
	}

	public static void main(String []args){
		//Creación
		Carro miCarro = new Carro("Ford");

		//Seteamos el kilometraje del carro
		miCarro.setKilometraje(2000);


		//Obtenemos el kilometraje del carro
		miCarro.getKilometraje( );

		//También podemos acceder a la variable de la clase
		System.out.println("Valor variable : "+miCarro.kilometraje);
	}
}