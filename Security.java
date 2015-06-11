package controllers;

import models.Configuracion;
import models.User;

import java.util.Hashtable;
import javax.naming.Context;
import javax.naming.directory.DirContext;
import javax.naming.NamingException;
import javax.naming.directory.InitialDirContext;

/**
 * Controlador de Autenticación
 * 
 * @author Gerardo Curiel <gcuriel@0269.com.ve>
 * 
 */
public class Security extends Secure.Security {

	// Metodo que autentifica usuario y contraseña
	// Cambiar por metodo de autenticacion ldap
	static boolean authentify(String username, String password) {


		User usuarioBd = User.connect(username, password); //autenticacion vs base de datos


		if (usuarioBd == null) {
			flash("errorBd", "Usuario No Registrado");
			return false;
		}

		//autenticacion vs ldap
        //System.out.println(usuarioBd);
		if (password != null && !password.isEmpty() && usuarioBd != null) {

			Hashtable env = new Hashtable();
			String servidor="ldap://161.196.64.2:389";// servidor de LDAP
			String dn=username+"@cantv.com.ve";

			env.put(Context.INITIAL_CONTEXT_FACTORY,"com.sun.jndi.ldap.LdapCtxFactory");
			env.put(Context.PROVIDER_URL, servidor);
			env.put(Context.SECURITY_AUTHENTICATION, "simple");
			env.put(Context.SECURITY_PRINCIPAL, dn);
			env.put(Context.SECURITY_CREDENTIALS, password);
			DirContext ctx = null;

			try {
				ctx = new InitialDirContext(env);
			} catch (NamingException e) {
				//System.out.println("Error Autenticando mediante LDAP, Error causado por : " + e.toString());
				return false;
			} finally {
				if (ctx != null) {
					try {
						ctx.close();
					} catch (Exception e) {
					}
				}
			}

			return true;

		}else{
			return false;
		}

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