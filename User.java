package models;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.ManyToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.hibernate.annotations.CacheConcurrencyStrategy;

import play.data.validation.Check;
import play.data.validation.CheckWith;
import play.data.validation.MaxSize;
import play.data.validation.Password;
import play.data.validation.Required;
import play.db.jpa.Model;

@Entity
@Table(name = "Usuario")
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class User extends Model {

	public boolean activo;
	public boolean administrador;

	@Required
	@MaxSize(255)
	public String nombre;
	@MaxSize(60)
	public String email;
	@Required
	@MaxSize(60)
	@CheckWith(UserCheck.class)
	public String usuario;
	@Required
	@MaxSize(255)
	@Password
	public String contrasena;

	@MaxSize(255)
	public String IDCentrosResponsabilidad;
	@MaxSize(255)
	public String descripcionCeco;
	@MaxSize(255)
	public String fechaIngreso;
	@MaxSize(255)
	public String cedula;
	@MaxSize(255)
	public String sexo;
	@MaxSize(255)
	public String idEmpleado;
	@MaxSize(255)
	public String fechaNacimiento;
	@MaxSize(255)
	public String departamento;
	@MaxSize(255)
	public String cargo;
	@MaxSize(255)
	public String supervisor;
	@MaxSize(255)
	public String entidadLegal;
	@MaxSize(255)
	public String divisionPersonal;
	@MaxSize(255)
	public String subdivisionPersonal;
	@MaxSize(255)
	public String areaNomina;
	@MaxSize(255)
	public String descripcionAreaNomina;
	@MaxSize(255)
	public String grupoPersonal;
	@MaxSize(255)
	public String descripcionGripoPersonal;
	@MaxSize(255)
	public String areaPersonal;
	@MaxSize(255)
	public String descripcionAreaPersonal;
	@MaxSize(255)
	public String posicion;
	@MaxSize(255)
	public String unidadOrganizativa;
	@MaxSize(255)
	public String descripcionUnidadOrganizativa;
	@MaxSize(255)
	public String gerenciaGeneral;

	@MaxSize(60)
	public String jefeCECO;

	@ManyToMany(cascade = CascadeType.ALL)
	@org.hibernate.annotations.BatchSize(size = 50)
	public Set<Rol> rol;

	@OneToOne
	@org.hibernate.annotations.BatchSize(size = 50)
	public Rol rolPrincipal;

	public User(String usuario, String email, String contrasena, String nombre) {
		this.email = email;
		this.contrasena = contrasena;
		this.nombre = nombre;
		this.usuario = usuario;
	}

	public static User connect(String username, String password) {
		return find("usuario = ? and activo = true ",
				username).first();
	}

	public String toString() {

		StringBuffer funcion = new StringBuffer();
		int i = 0;

		if (rol != null) {

			Iterator<Rol> it = rol.iterator();

			while (it.hasNext()) {

				Rol tmp = it.next();

				if (tmp.descripcion.equals("admin"))
					funcion.append("Administrador");
				else if (tmp.descripcion.equals("encuestado"))
					funcion.append("Encuestado");
				else if (tmp.descripcion.equals("generador"))
					funcion.append("Generador");
				else if (tmp.descripcion.equals("seguridad"))
					funcion.append("Seguridad");
				else
					funcion.append("Funcional");

				i++;

				if (i != rol.size())
					funcion.append(",");
			}
		}

		return nombre + "(" + funcion.toString() + ")";
	}

	static class UserCheck extends Check {

		public boolean isSatisfied(Object validatedObject, Object value) {

			// Usuario a Salvar
			User userT = (User) validatedObject;

			String usuario = (String) value;

			List<User> tmp = User.find("byUsuario", usuario).fetch();
			Iterator<User> it = tmp.iterator();

			if (tmp.size() == 0)
				return true;
			else {

				while (it.hasNext()) {

					User plant = it.next();

					if (!plant.equals(userT)) {
						setMessage("user.errorusuario");
						return false;
					}
				}
			}

			return true;
		}
	}

}