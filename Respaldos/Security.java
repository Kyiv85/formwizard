package controllers;

import models.Configuracion;
import models.User;

/**
 * Controlador de Autenticación
 * 
 * @author Gerardo Curiel <gcuriel@0269.com.ve>
 * 
 */
public class Security extends Secure.Security {

	static boolean authentify(String username, String password) {
		return User.connect(username, password) != null;
	}

	static boolean check(String profile) {

		Configuracion conf = Configuracion.findById((long) 1);

		User u = User
				.find("usuario = ?  and administrador = true", connected())
				.<User> first();

		if (u != null) {
			return true;
		}

		User usr = User.find("usuario = ? and activo = true ", connected())
				.<User> first();

		// Administrador
		if ("admin".equals(profile)) {
			if (conf.__AdminActivo) {

				if (usr.rolPrincipal == null)
					return false;
				else
					return usr.rolPrincipal.descripcion.equals("admin");

			} else {
				try {
					Secure.logout();
				} catch (Throwable e) {
					e.printStackTrace();
				}
			}
			// Funcional
		} else if ("funcional".equals(profile)) {
			if (conf.__FuncionalActivo) {

				if (usr.rolPrincipal == null)
					return false;
				else
					return usr.rolPrincipal.descripcion.equals("funcional");

			} else {
				try {
					Secure.logout();
				} catch (Throwable e) {
					e.printStackTrace();
				}
			}

			// Encuestado
		} else if ("encuestado".equals(profile)) {
			if (conf.__EncuestadoActivo) {

				if (usr.rolPrincipal == null)
					return false;
				else
					return usr.rolPrincipal.descripcion.equals("encuestado");

			} else {
				try {
					Secure.logout();
				} catch (Throwable e) {
					e.printStackTrace();
				}
			}

			// Generador
		} else if ("generador".equals(profile)) {
			if (conf.__GeneradorActivo) {

				if (usr.rolPrincipal == null)
					return false;
				else
					return usr.rolPrincipal.descripcion.equals("generador");

			} else {
				try {
					Secure.logout();
				} catch (Throwable e) {
					e.printStackTrace();
				}
			}

			// Seguridad
		} else if ("seguridad".equals(profile)) {
			if (usr.rolPrincipal == null)
				return false;
			else
				return usr.rolPrincipal.descripcion.equals("seguridad");
		}

		return false;
	}

	static void onDisconnected() {
		Application.index();
	}

	static void onAuthenticated() {
		Application.index();
	}

}