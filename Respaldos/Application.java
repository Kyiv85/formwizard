package controllers;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import models.Configuracion;
import models.Encuesta;
import models.EncuestaForm;
import models.PermisoEncuesta;
import models.Rol;
import models.Status;
import models.TipoEncuesta;
import models.User;
import play.Logger;
import play.mvc.Controller;
import play.mvc.With;
import sun.org.mozilla.javascript.internal.IdScriptableObject;
import utils.Utilities;

import com.google.gson.Gson;

import db.Driver;
import db.UUIDGenerator;

/**
 * Controlador principal de SASWeb
 *
 * @author Gerardo Curiel <gcuriel@0269.com.ve>
 *
 */
@With(Secure.class)
public class Application extends Controller {

	/**
	 * Index de la aplicación
	 *
	 * Se realiza la redirección al módulo correspondiente dependiendo del Rol.
	 * Si un usuario posee mas de un rol, se muestra un menú para elegir el rol
	 * principal en esa sesión.
	 *
	 */
	static String idSeparator = ";";
	
	public static void index() {

		String user = Security.connected();
		User usr = User.find("usuario = ? and activo = true ", user)
				.<User> first();

		if (usr != null) {
			if (usr.rol.size() > 1) {

				usr.rolPrincipal = null;
				usr.save();
				session.put("user", usr);
				main();

			} else {
				usr.rolPrincipal = usr.rol.iterator().next();
				usr.save();
				redirectMain();
			}
		}
		redirect("/secure/login");
	}

	/**
	 * Redirección. Necesaria para usuarios con multiples roles.
	 *
	 */
	public static void redirectMain() {

		String user = Security.connected();
		User usr = User.find("usuario = ? and activo = true ", user)
				.<User> first();

		Configuracion conf = Configuracion.findById((long) 1);

		if (usr.rolPrincipal.descripcion.equals("admin")) {

			// Si el modulo está activo o eres el Administrador
			// Administrador
			if (conf.__AdminActivo || usr.administrador) {

				session.put("user", usr);
				Logger.info("Ha ingresado el usuario " + usr
						+ " con el rol de administrador");
				redirect("/SASWeb/gwt-public/adminsas/admin.html");
			} else {

				render("errors/disabled.html");
			}

		} else if (usr.rolPrincipal.descripcion.equals("encuestado")) {

			// Si el modulo está activo o eres el Administrador
			// Encuestado
			if (conf.__EncuestadoActivo || usr.administrador) {

				session.put("user", usr);
				Logger.info("Ha ingresado el usuario " + usr
						+ " con el rol de encuestado");
				Encuestado.encuestado();
			} else {

				render("errors/disabled.html");
			}

		} else if (usr.rolPrincipal.descripcion.equals("funcional")) {

			// Si el modulo está activo o eres el Administrador
			// Funcional
			if (conf.__FuncionalActivo || usr.administrador) {

				session.put("user", usr);
				Logger.info("Ha ingresado el usuario " + usr
						+ " con el rol de funcional");
				funcional();
			} else {

				render("errors/disabled.html");
			}

		} else if (usr.rolPrincipal.descripcion.equals("generador")) {

			// Si el modulo está activo o eres el Administrador
			// Encuestado
			if (conf.__GeneradorActivo || usr.administrador) {

				session.put("user", usr);
				Logger.info("Ha ingresado el usuario " + usr
						+ " con el rol de generador");
				redirect("/SASWeb/admin/");
			} else {

				render("errors/disabled.html");
			}
		} else if (usr.rolPrincipal.descripcion.equals("seguridad")) {

			session.put("user", usr);
			Logger.info("Ha ingresado el usuario " + usr
					+ " con el rol de seguridad");
			redirect("/SASWeb/seguridad/list");
		}
	}

	/**
	 * Elección de roles
	 */
	public static void main() {

		Logger.info("El usuario " + Security.connected()
				+ " accedió a la vista de elección de roles");

		String usr = Security.connected();
		User user = User.find("byUsuario", usr).<User> first();
		Set<Rol> roles = user.rol;

		render(roles);
	}

	/**
	 * Cambio de rol y redirección
	 */
	public static void changeRole(String id) {

		Logger.info("El usuario " + Security.connected()
				+ " cambio su rol Principal");

		String usr = Security.connected();
		User user = User.find("byUsuario", usr).<User> first();

		Rol principal = Rol.findById(new Long(id));

		if (principal != null) {

			// Si no es un rol forzado que no ha sido asignado
			if (user.rol.contains(principal)) {

				user.rolPrincipal = principal;
				user.save();
				redirectMain();
			}
		}

		main();
	}

	/**
	 * Vista de Administrador SASWebAdmin
	 */
	@Check("admin")
	public static void interceptAdmin(String url) {
		Logger.info("El usuario " + Security.connected()
				+ " accedió a la vista Administrador, URI: "
				+ "gwt-public/adminsas/admin.html");
		redirect("/gwt-public/adminsas/admin.html");
	}

	/**
	 * Index de usuario funcional
	 */
	@Check("funcional")
	public static void funcional() {

		Logger.info("El usuario " + Security.connected()
				+ " accedió a la vista Funcional");

		String usr = Security.connected();
		User user = User.find("byUsuario", usr).<User> first();

		// Enviado a Funcional
		Status envi = Status.findById((long) 2);
		// Validado por Funcional
		Status validado = Status.findById((long) 3);
		// Rechazado por Funcional
		Status rechazado = Status.findById((long) 4);

		// Tipo "Encuesta"
		TipoEncuesta tipoEncuesta = TipoEncuesta.findById((long) 2);
		// Ordenes
		TipoEncuesta tipoOrdenes = TipoEncuesta.findById((long) 1);

		List<PermisoEncuesta> noactualizados = PermisoEncuesta
				.find("(status = ? or status = ?) and revisor = ? and __Activo = true and " +
						"(encuesta.tipo = ? or encuesta.tipo = ? )",
						envi, rechazado, user, tipoEncuesta, tipoOrdenes).fetch();

		List<PermisoEncuesta> actualizados = PermisoEncuesta
				.find("status = ?  and revisor = ? and __Activo = true and " +
						"(encuesta.tipo = ? or encuesta.tipo = ? )",
						validado, user, tipoEncuesta, tipoOrdenes).fetch();

		render(noactualizados, actualizados);
	}

	/**
	 * Menu de Estadisticos
	 */
	@Check("funcional")
	public static void estadisticos() {

		Logger.info("El usuario " + Security.connected()
				+ " accedió a la vista de Estadistico");

		String usr = Security.connected();
		User user = User.find("byUsuario", usr).<User> first();

		// Enviado a Encuestado
		Status enviEncuestado = Status.findById((long) 1);
		// Enviado a Funcional
		Status envi = Status.findById((long) 2);
		// Validado por Funcional
		Status validado = Status.findById((long) 3);
		// Rechazado por Funcional
		Status rechazado = Status.findById((long) 4);

		// Tipo "Encuesta"
		TipoEncuesta tipoEncuesta = TipoEncuesta.findById((long) 2);
		// Ordenes
		TipoEncuesta tipoOrdenes = TipoEncuesta.findById((long) 1);

		List<PermisoEncuesta> actualizados = PermisoEncuesta
				.find("(status = ? or status = ?) and revisor = ? and __Activo = true and" +
						" (encuesta.tipo = ?  or encuesta.tipo = ? ) ",
						envi, rechazado, user, tipoOrdenes, tipoEncuesta).fetch();

		// No Actualizados
		List<PermisoEncuesta> noactualizados = PermisoEncuesta
				.find("(status = ? or status = ?) and revisor = ? and __Activo = true and" +
						" (encuesta.tipo = ? or encuesta.tipo = ? ) ",
						enviEncuestado, validado, user, tipoOrdenes, tipoEncuesta).fetch();

		ArrayList<HashMap<String, String>> noact = getEstadisticoLines(noactualizados, false);
		ArrayList<HashMap<String, String>> act = getEstadisticoLines(noactualizados, true);

		render(noactualizados, actualizados, noact, act);
	}

	/**
	 * Lineas de Estadistico
	 */
	public static ArrayList<HashMap<String, String>> getEstadisticoLines(List<PermisoEncuesta> listaEncuestas, boolean activo) {


		ArrayList<HashMap<String, String>> items = new ArrayList<HashMap<String, String>>();
		ArrayList<HashMap<String, String>> list = null;

		// Todas las encuestas de Funcional		
		Iterator<PermisoEncuesta> listEnc = listaEncuestas.iterator();

		while (listEnc.hasNext()) {
			
			PermisoEncuesta perm = listEnc.next();
			Encuesta encuesta = perm.encuesta;

			try {
				Driver driver = new Driver();

				// Lista de EncuestaForm (preguntas) de cada Encuesta
				List<EncuestaForm> preguntas = encuesta.encuestas;
				Iterator<EncuestaForm> preguntasIte = preguntas.iterator();

				String[] keys = new String[] { "DestinoDimMemberRef1",
						"DestinoDimMemberRef2", "DestinoDimMemberRef3",
						"DestinoDimMemberRef4", "DestinoDimMemberRef5",
						"DestinoDimMemberRef6", "DestinoDimMemberRef7",
						"DestinoDimMemberRef8", "DestinoDimMemberRef9",
						"DestinoDimMemberRef10", "DestinoDimMemberRef11",
						"DestinoDimMemberRef12" };

				// Pido preguntas por cada EncuestaForm
				while (preguntasIte.hasNext()) {

					EncuestaForm tmp = preguntasIte.next();
					
					// Lista de Items
					ArrayList<HashMap<String, String>> result = driver.getFilasEncuesta(perm.modelo, perm.periodo, perm.ceco, tmp.nombreConductor, activo);
					Iterator<HashMap<String, String>> rowList = result
							.iterator();

					while (rowList.hasNext()) {

						HashMap<String, String> tmpRow = rowList.next();
						StringBuilder sb = new StringBuilder();

						for (String s : keys) {

							String dimension = tmpRow.get(s);

							if (!dimension.equals("")) {
								sb.append(dimension);
								sb.append("-");
							}
						}
						sb.deleteCharAt(sb.length() - 1);
						tmpRow.put("Nombre_Actividad", sb.toString());
						tmpRow.put("perm_id", perm.id.toString());
						tmpRow.put("periodo", perm.periodo);
						tmpRow.put("status", perm.status.toString());
					}
					items.addAll(result);

				}

				// Unir lineas iguales
				list = Utilities.getMergedListEstadistico(items);

				// Termino de obtener items
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return list;
	}

	/**
	 * Llenar estadistico
	 */
	@Check("funcional")
	public static void llenarEstadistico(int idPermiso) {

		Logger.info("El usuario " + Security.connected()
				+ " accedió a la vista de llenar Estadistico");

		ArrayList<HashMap<String, String>> actividades;
		ArrayList<HashMap<String, String>> estadistico;

		HashMap<String, List> selectBoxes;

		String dimFuente = params.get("dimFuente");

		PermisoEncuesta perm = PermisoEncuesta.findById((long) idPermiso);
		Encuesta encuesta = perm.encuestaValidador;

		String modelo = perm.modelo;
		
		String[] keys = new String[] { "DestinoDimMemberRef1",
				"DestinoDimMemberRef2", "DestinoDimMemberRef3",
				"DestinoDimMemberRef4", "DestinoDimMemberRef5",
				"DestinoDimMemberRef6", "DestinoDimMemberRef7",
				"DestinoDimMemberRef8", "DestinoDimMemberRef9",
				"DestinoDimMemberRef10", "DestinoDimMemberRef11",
				"DestinoDimMemberRef12" };
		
		Driver driv = new Driver();

		// ID, Nombre
		actividades = driv.getObjetoCosto(encuesta.modelo, perm.periodo,
				perm.escenario);

		estadistico = driv.getEncuestaRowDataSingle(modelo, dimFuente);

		Iterator<HashMap<String, String>> rowList = estadistico.iterator();

		while (rowList.hasNext()) {

			HashMap<String, String> tmpRow = rowList.next();
			StringBuilder sb = new StringBuilder();

			for (String s : keys) {

				String dimension = tmpRow.get(s);

				if (!dimension.equals("")) {
					sb.append(dimension);
					sb.append("-");
				}
			}
			sb.deleteCharAt(sb.length() - 1);
			tmpRow.put("Nombre_Actividad", sb.toString());
		}

		selectBoxes = new HashMap<String, List>();

		for (int i = 0; i < actividades.size(); i++) {

			String dim = actividades.get(i).get("id");
			selectBoxes.put(dim, driv.getActividadesData(dim, encuesta.modelo,
					perm.periodo, perm.escenario));
		}

		render(estadistico, perm, encuesta, actividades, selectBoxes, dimFuente);
	}

	/**
	 * Llenar Encuesta Funcional
	 */
	@Check("funcional")
	public static void llenarEncuestaFuncional(int idPermiso, int idEncuesta) {

		Logger.info("El usuario " + Security.connected()
				+ " accedió a la vista de llenar Encuesta");

		// Enviar encuesta, encuesta.encuestas(encuestaform),
		ArrayList<HashMap<String, String>> actividades;
		HashMap<String, List> selectBoxes;

		PermisoEncuesta perm = PermisoEncuesta.findById((long) idPermiso);
		Encuesta encuesta = Encuesta.findById((long) idEncuesta);

		Driver driv = new Driver();

		// ID, Nombre
		actividades = driv.getActividades(encuesta.modelo, perm.periodo,
				perm.escenario);
		selectBoxes = new HashMap<String, List>();

		for (int i = 0; i < actividades.size(); i++) {

			String dim = actividades.get(i).get("id");
			selectBoxes.put(dim, driv.getActividadesData(dim, encuesta.modelo,
					perm.periodo, perm.escenario));

		}

		render("Encuestado/llenarEncuesta.html", perm, encuesta, actividades,
				selectBoxes);
	}

	/**
	 * Guarda Encuesta
	 */
	@Check("encuestado")
	public static void guardarEncuesta(int id) {

		Logger.info("El usuario " + Security.connected()
				+ " accedió a la vista de Guardar Encuesta");
		Encuestado.encuestado();
	}

	/**
	 * Vista de Generador
	 */
	@Check("generador")
	public static void generador() {

		Logger.info("El usuario " + Security.connected()
				+ " accedió a la vista de Encuestado");
		List<Encuesta> noactualizados = Encuesta.findAll();

		render(noactualizados);
	}

	/**
	 * Uso en CRUD de PermisoEncuesta
	 */
	public static void showEncuestaData(int id) {

		Encuesta encuesta = Encuesta.findById((long) id);

		Driver driver = new Driver();

		ArrayList<HashMap<String, String>> result = 
			                    driver.getConductor(encuesta.modelo);

		Map<String, Object> map = new HashMap<String, Object>();
		map.put("modelo", encuesta.modelo);
		map.put("tipo", encuesta.tipo.descripcion);
		map.put("conductor", Utilities.getArrayMap(result, "Nombre", "ID"));

		renderJSON(map);
	}

	/**
	 * Uso en CRUD de PermisoEncuesta. Lista Datos de filas de encuesta
	 */
	public static void listEncuestaRowData(String idPermiso, String idPregunta) {

		int i = 0;
		float dqfTotal = 0;

		String periodo = params.get("periodo");
		String ceco = params.get("ceco");
		String conductor = "";
		String modelo = "";

		Map<String, Object> map = new HashMap<String, Object>();
		Map<String, Object> userdata = new HashMap<String, Object>();

		Driver driver = new Driver();

		try {
			
			PermisoEncuesta permEncuesta = PermisoEncuesta.findById(new Long(idPermiso));
			EncuestaForm pregunta = EncuestaForm.findById(new Long(idPregunta));
		
			modelo = permEncuesta.modelo;
			conductor = pregunta.nombreConductor;
			
			ArrayList<HashMap<String, String>> result = 
                                      driver.getFilasEncuesta(modelo, periodo, ceco, conductor, false);

			ArrayList<HashMap<String, Object>> rows = new ArrayList<HashMap<String, Object>>();
			Iterator<HashMap<String, String>> it = result.iterator();

			while (it.hasNext()) {

				HashMap<String, String> tmp = it.next();
				HashMap<String, Object> row = new HashMap<String, Object>();

				row.put("id", periodo+ idSeparator + ceco + idSeparator + conductor +  idSeparator +tmp.get("DimensionDestino"));
				row.put("cell", tmp);
				rows.add(row);
				i++;
			}

			HashMap<String, String> dqfSum = 
				            driver.getDQFSum(modelo, periodo, ceco, conductor);

			if (!dqfSum.get("Total").equals("")) {
				dqfTotal = Float.parseFloat(dqfSum.get("Total")); 
			}

			userdata.put("cell.DestinoDimMemberRef1", "Total:");
			userdata.put("cell.DQF", dqfTotal);

			map.put("total", 1);
			map.put("pages", 1);
			map.put("records", result.size());

			map.put("rows", rows);
			map.put("userdata", userdata);
			
		} catch (Exception e) {
			e.printStackTrace();
		}

		renderJSON(map);
	}

	/**
	 * Lista filas de datos de Estadistico
	 */
	public static void listEncuestaRowDataEstadistico(String idPermiso, String idPregunta) {

		int i = 0;
		float dqfTotal = 0;
		
		String fuente = params.get("fuente");
		String conductor = params.get("conductor");
		
		Map<String, Object> map = new HashMap<String, Object>();
		ArrayList<HashMap<String, Object>> rows = new ArrayList<HashMap<String, Object>>();
		Map<String, Object> userdata = new HashMap<String, Object>();
		
		Driver driver = new Driver();

		try {
			PermisoEncuesta permEncuesta = PermisoEncuesta.findById(new Long(idPermiso));

			ArrayList<HashMap<String, String>> result = driver.getEstadisticoRowDataSingle(permEncuesta.modelo, fuente, conductor);

			Iterator<HashMap<String, String>> it = result.iterator();
			
			while (it.hasNext()) {

				HashMap<String, String> tmp = it.next();
				HashMap<String, Object> row = new HashMap<String, Object>();

				row.put("id", tmp.get("DimensionDestino"));
				row.put("cell", tmp);
				rows.add(row);
				i++;
			}

			HashMap<String, String> dqfSum = 
				            driver.getDQFSumEstadistico(permEncuesta.modelo, permEncuesta.periodo, fuente, conductor);

			if (!dqfSum.get("Total").equals("")) {
				dqfTotal = Float.parseFloat(dqfSum.get("Total")); 
			}

			userdata.put("cell.DestinoDimMemberRef1", "Total:");
			userdata.put("cell.DQF", dqfTotal);

			map.put("total", 1);
			map.put("pages", 1);
			map.put("records", result.size());

			map.put("rows", rows);
			map.put("userdata", userdata);

			
		} catch (Exception e) {
			e.printStackTrace();
		}

		renderJSON(map);
	}

	/**
	 * Inserta filas de datos de Estadistico
	 */
	public static void insertEncuestaRowDataEstadistico(int idPermiso,
			int idPregunta) {

		String activo = "0";
		String tipoModuloABCFuente = "ACTIVITY";
		String tipoModuloABCDestino = "COSTOBJECT";
		String dimensionDestino = "";
		String DQF;
		StringBuffer dimValues = new StringBuffer();

		boolean success = false;
		
		ArrayList<String> dimDestino = new ArrayList<String>();

		PermisoEncuesta perm = PermisoEncuesta.findById((long) idPermiso);

		// Obtener parametros
		String modelo = perm.modelo;
		String periodo = perm.periodo;
		String escenario = perm.escenario;

		String dimFuente = params.get("dimFuente");
		
		Driver driv = new Driver();
		Gson gson = new Gson();

		EncuestaForm encuestaPreg = EncuestaForm.findById((long) idPregunta);
		String conductor = encuestaPreg.nombreConductor;

		// Dimension Fuente
		String choices = params.get("choices", String.class);
		Data[] decoded = gson.fromJson(choices, Data[].class);

		// Tomar toda la data menos la Medida
		for (int i = 0; i < decoded.length - 1; i++) {
			dimDestino.add(decoded[i].value);

			if (dimensionDestino.equals("")) {
				dimensionDestino = decoded[i].value;
			} else {
				dimensionDestino = dimensionDestino + "-" + decoded[i].value;
			}
		}

		for (int i = 0; i < 12; i++) {
			String value;
			try {
				value = dimDestino.get(i);
				dimValues.append("'" + value + "',");

			} catch (IndexOutOfBoundsException e) {
				dimValues.append("null,");
			}
		}

		// DQF
		DQF = decoded[decoded.length - 1].value;

		String bd = Driver.getModelNameAsDB(modelo);

		HashMap<String, Object> map = new HashMap<String, Object>();
		
		StringBuffer dimFuenteValues = new StringBuffer();
		String [] dimFuenteParsed = dimFuente.split("-");
		
		// Parseo de dimFuente
		for (int i = 0; i < 12; i++) {
			String value;
			try {
				
				value = dimFuenteParsed[i];
				dimFuenteValues.append("'" + value + "',");

			} catch (IndexOutOfBoundsException e) {
				dimFuenteValues.append("null,");
			}
		}

		String sql = "INSERT INTO "
				+ bd
				+ ".dbo.asignacionTemporal ( "
				+ "__Activo, _IDPeriodo, _NombreModelo, _IDEscenario, DimensionFuente, "
				+ "FuenteDimMemberRef1, FuenteDimMemberRef2, FuenteDimMemberRef3,  FuenteDimMemberRef4,  FuenteDimMemberRef5, "
				+ "FuenteDimMemberRef6, FuenteDimMemberRef7, FuenteDimMemberRef8,  FuenteDimMemberRef9,  FuenteDimMemberRef10, "
				+ "FuenteDimMemberRef11, FuenteDimMemberRef12, "
				+ "_TipoModuloABCFuente, _NombreConductor, DimensionDestino, "
				+ "DestinoDimMemberRef1, DestinoDimMemberRef2,DestinoDimMemberRef3,DestinoDimMemberRef4,DestinoDimMemberRef5,"
				+ "DestinoDimMemberRef6,DestinoDimMemberRef7,DestinoDimMemberRef8,DestinoDimMemberRef9, DestinoDimMemberRef10, "
				+ "DestinoDimMemberRef11, DestinoDimMemberRef12, "
				+ "_TipoModuloABCDestino, DQF, _idPermisoEncuesta, _idPregunta,  fechaAsignacion)"
				+ "values (" + "'" + activo + "', " + "'" + periodo + "', "
				+ "'" + modelo + "', " + "'" + escenario + "', " + "'"
				+ dimFuente + "', " + dimFuenteValues.toString() + "'"
				+ tipoModuloABCFuente + "', " + "'" + conductor + "', " + "'"
				+ dimensionDestino + "', " + dimValues.toString() + "'"
				+ tipoModuloABCDestino + "', " + "'" + DQF + "', '" + idPermiso
				//+ "', '" + idPregunta + "', '" + Utilities.getTimeStamp()+"')";//error en timestamp entre formato BBDD y CONTROLLER
				+ "', '" + idPregunta + "', GETDATE())";
		
		
		Logger.info(sql);
		success = driv._queryWithoutResult(sql);

		if (success) {
			map.put("success", "true");
		} else {
			map.put("success", "false");
			map.put("message",
					"Asignación ya existe. Por favor seleccionar otras combinación");
		}

		renderJSON(map);
	}

	/**
	 * Inserta filas en encuesta
	 */
	public static void insertEncuestaRowData(int idPermiso, int idPregunta) {

		Logger.debug("Application.insertEncuestaRowData");

		String activo = "0";
		String DQF;

		Driver driv = new Driver();

		PermisoEncuesta perm = PermisoEncuesta.findById((long) idPermiso);

		EncuestaForm encuestaPreg = EncuestaForm.findById((long) idPregunta);
		String conductor = encuestaPreg.nombreConductor;

		// Obtener parametros
		String modelo = perm.modelo;
		String periodo = perm.periodo;
		String escenario = perm.escenario;
		String ceco = perm.ceco;

		StringBuffer dimValues = new StringBuffer();

		// Dimension Fuente
		String tipoModuloABCFuente = "RESOURCE";
		ArrayList<HashMap<String, String>> filales = driv.getFilial();

		ArrayList<HashMap<String, String>> ctas = driv.getCuenta(conductor,	modelo);

		// Si no existe conductor asociado
		HashMap<String, Object> map = new HashMap<String, Object>();
		if (ctas.size() == 0) {
			map.put("success", "false");
			map.put("message",
					"No existe conductor asociado. Por favor seleccionar otras combinación");
			renderJSON(map);
		}

		String cta = ctas.get(0).get("GrupoCuenta");

		String choices = params.get("choices", String.class);

		Gson gson = new Gson();
		Data[] decoded = gson.fromJson(choices, Data[].class);

		// Get Dimension Destino
		String tipoModuloABCDestino = "ACTIVITY";
		ArrayList<String> dimDestino = new ArrayList<String>();
		String dimensionDestino = "";

		// Tomar toda la data menos la Medida
		for (int i = 0; i < decoded.length - 1; i++) {
			dimDestino.add(decoded[i].value);
			if (dimensionDestino.equals("")) {
				dimensionDestino = decoded[i].value;
			} else {
				dimensionDestino = dimensionDestino + "-" + decoded[i].value;
			}
		}

		for (int i = 0; i < 12; i++) {
			String value;
			try {
				value = dimDestino.get(i);
				dimValues.append("'" + value + "',");

			} catch (IndexOutOfBoundsException e) {
				dimValues.append("null,");
			}
		}

		// DQF
		DQF = decoded[decoded.length - 1].value;

		String filial = null;
		String sql = null;
		String DimensionFuente = null;
		String bd = Driver.getModelNameAsDB(modelo);

		boolean success = false;

		String idInsert = UUIDGenerator.getUUID();

		// Si el tipoValor es un porcentaje
		if (encuestaPreg.tipoValor.nombre.equals("Porcentaje")) {
			
			// Obtener total de DQF de una pregunta
			HashMap<String, String> dqfSum = driv.getDQFSum(modelo, periodo, ceco, conductor);

			if (!dqfSum.get("Total").equals("")) {

				float dqfTotal = Float.parseFloat(dqfSum.get("Total"));
				int totalAgregado = (int) dqfTotal + Integer.parseInt(DQF);
				// lo que llevamos mas el nuevo es mayor a 100
				// Se debe tomar en cuenta la cantidad de filiales
				if (totalAgregado > 100) {

					map.put("success", "false");
					map.put("message", "El total no puede ser mayor a 100%");
					renderJSON(map);
				}
				
			}

		}

		for (int i = 0; i < filales.size(); i++) {

			filial = filales.get(i).get("ID");
			DimensionFuente = filial + "-" + ceco + "-" + cta;



			sql = "INSERT INTO "
					+ bd
					+ ".dbo.asignacionTemporal ( "
					+ "__Activo, _IDPeriodo, _NombreModelo, _IDEscenario, DimensionFuente, "
					+ "FuenteDimMemberRef1, FuenteDimMemberRef2, FuenteDimMemberRef3, "
					+ "_TipoModuloABCFuente, _NombreConductor, DimensionDestino, "
					+ "DestinoDimMemberRef1, DestinoDimMemberRef2,DestinoDimMemberRef3,DestinoDimMemberRef4,DestinoDimMemberRef5,"
					+ "DestinoDimMemberRef6,DestinoDimMemberRef7,DestinoDimMemberRef8,DestinoDimMemberRef9, DestinoDimMemberRef10, "
					+ "DestinoDimMemberRef11, DestinoDimMemberRef12, "
					+ "_TipoModuloABCDestino, DQF, _idPermisoEncuesta, _idPregunta,  fechaAsignacion, _idInsert )"
					+ "values (" + "'" + activo + "', " + "'" + periodo + "', "
					+ "'" + modelo + "', " + "'" + escenario + "', " + "'"
					+ DimensionFuente + "', " + "'" + filial + "', " + "'"
					+ ceco + "', " + "'" + cta + "', " + "'"
					+ tipoModuloABCFuente + "', " + "'" + conductor + "', "
					+ "'" + dimensionDestino + "', " + dimValues.toString()
					+ "'" + tipoModuloABCDestino + "', " + "'" + DQF + "', '"
					+ idPermiso + "', '" + idPregunta + "',"
					//+ Utilities.getTimeStamp() + "' , '" + idInsert + "') ;"; //error en timestamp entre formato BBDD y CONTROLLER
                    + " GETDATE() ,'" + idInsert + "') ;";
			Logger.info(sql) ;
			success = driv._queryWithoutResult(sql);

			if (!success)
				break;
		}

		
		if (success) {
			map.put("success", "true");
		} else {
			map.put("success", "false");
			map.put("message",
					"Asignación ya existe. Por favor seleccionar otras combinación");
		}
		
		renderJSON(map);
	}

	/**
	 * Modifica filas en encuesta
	 * Req
	 * EP 18032015
	 */
	public static void actualizaEncuestaRowData(int idPermiso, int idPregunta) {

		boolean success = false;
		String sql = null;
		

		String data[] = params.get("choices").split("%3B");
		String datas[] = data[0].split(";");
		String datas2[] = datas[3].split(",");
		Logger.info("variable data " + data[0] + ".");
		Logger.info("variable datas " + datas[2] + ".");
		Logger.info("variable datas2 " + datas2[0] + ".");
		String periodo = datas[0];
		periodo = periodo.substring(7);
		String ceco = params.get("ceco");
		String dqfn = params.get("dqf");
		Logger.info("variable periodo " + periodo + ".");
		Logger.info("variable ceco " + ceco + ".");
		Logger.info("variable ceco " + dqfn + ".");

		String conductor = datas[2];
		String dimensionDestino = datas2[0];
		dimensionDestino = dimensionDestino.substring(0,dimensionDestino.length()-1);
		Logger.info("variable data conductor " + conductor + ".");
		Logger.info("variable data dimensiondestino " + dimensionDestino + ".");
		

	
		PermisoEncuesta perm = PermisoEncuesta.findById((long) idPermiso);
	
	    Logger.info("El usuario " + Security.connected()
	            + " accedió a actualizarEncuestaRowData");
	
		Driver driv = new Driver();
	
		HashMap<String, Object> map = new HashMap<String, Object>();
	
		String modelo = perm.modelo;
		String bd = Driver.getModelNameAsDB(modelo);
	
		sql = "UPDATE " + bd + ".dbo.asignacionTemporal" + " SET DQF = '"+dqfn+"' WHERE "
		 	+ "_IDPeriodo = '" + periodo + "' "
		    + " AND FuenteDimMemberRef2 = '" + ceco + "' "
		    + " AND FuenteDimMemberRef3  COLLATE Traditional_Spanish_CI_AS in  " +
		    		"(SELECT distinct grupoCuenta from " + bd + ".dbo.EquivalenciaGC " +
		    	      " WHERE _IDConductor = '" + conductor + "') "
		    + " AND _NombreConductor = '" + conductor + "' "
		    + " AND DimensionDestino = '" + dimensionDestino + "' ";
		    
		Logger.info(sql);
		success = driv._queryWithoutResult(sql);
	
		if (success)
			map.put("success", "true");
		else
			map.put("success", "false");
	
		renderJSON(map);
	}
	
	/**
	 * Modifica filas en encuesta
	 * Req
	 * EP 18032015
	 */
	public static void actualizaEncuestaRowDataEst(int idPermiso, String fuente, String conductor, String id) {

		boolean success = false;
		String sql = null;
		HashMap<String, Object> map = new HashMap<String, Object>();

		PermisoEncuesta perm = PermisoEncuesta.findById((long) idPermiso);

		String data[] = params.get("choices").split("%3B");
		String datas[] = data[0].split(";");

		Logger.info("variable data " + data[0] + ".");
		
		String dqf = params.get("dqf");

		Driver driv = new Driver();

		String modelo = perm.modelo;
		String bd = Driver.getModelNameAsDB(modelo);
		String periodo = perm.periodo;
		String DimensionDestino = data[0];
		DimensionDestino = DimensionDestino.substring(2,DimensionDestino.length()-2);
		
		sql = "UPDATE " + bd + ".dbo.asignacionTemporal" + " SET "
			+ "DQF = '" + dqf
		 	+ "' WHERE _IDPeriodo = '" + periodo + "' "
		    + " AND DimensionFuente = '" + fuente + "' "
		    + " AND _NombreConductor = '" + conductor + "' "
		    + " AND DimensionDestino = '" + DimensionDestino + "' ";
		
		Logger.info(sql);
		success = driv._queryWithoutResult(sql);
	
		if (success)
			map.put("success", "true");
		else
			map.put("success", "false");
	
		renderJSON(map);
	}

	
	/**
	 * Borra filas en encuesta
	 */
	public static void deleteEncuestaRowData(int idPermiso, int idPregunta) {
	
		boolean success = false;
		String sql = null;
	
		String data[] = params.get("id").split(idSeparator);
		
		String periodo = data[0];
		String ceco = data[1];
		String conductor = data[2];
		String dimensionDestino = data[3];
	
		PermisoEncuesta perm = PermisoEncuesta.findById((long) idPermiso);
	
	    Logger.info("El usuario " + Security.connected()
	            + " accedió a deleteEncuestaRowData");
	
		Driver driv = new Driver();
	
		HashMap<String, Object> map = new HashMap<String, Object>();
	
		String modelo = perm.modelo;
		String bd = Driver.getModelNameAsDB(modelo);
	
		sql = "DELETE FROM " + bd + ".dbo.asignacionTemporal" + " WHERE "
		 	+ "_IDPeriodo = '" + periodo + "' "
		    + " AND FuenteDimMemberRef2 = '" + ceco + "' "
		    + " AND FuenteDimMemberRef3  COLLATE Traditional_Spanish_CI_AS in  " +
		    		"(SELECT distinct grupoCuenta from " + bd + ".dbo.EquivalenciaGC " +
		    	      " WHERE _IDConductor = '" + conductor + "') "
		    + " AND _NombreConductor = '" + conductor + "' "
		    + " AND DimensionDestino = '" + dimensionDestino + "' ";
		    
		Logger.info(sql);
		success = driv._queryWithoutResult(sql);
	
		if (success)
			map.put("success", "true");
		else
			map.put("success", "false");
	
		renderJSON(map);
	}

	/**
     * Borra filas en encuesta EP
     */
    public static void deleteEncuestaRowDatas(int idPermiso, int idPregunta) {

        boolean success = false;
        String sql = null;
        String dimensionDestino = "";

        String data[] = params.get("id").split(",");
        String datas[] = data[0].split(idSeparator);

        String periodo = datas[0];
        String ceco = datas[1];
        String conductor = datas[2];
        int i;

        for (i = 0; i < data.length; i++) {

            String datax[] = data[i].split(idSeparator);
            if (dimensionDestino.equals("")) {
                dimensionDestino = "'"+datax[3]+"'";
            } else {
                dimensionDestino = dimensionDestino +",'"+datax[3]+"'";
            }
            }


        //dimensionDestino = datas[3];

        PermisoEncuesta perm = PermisoEncuesta.findById((long) idPermiso);

        Logger.info("El usuario " + Security.connected()
                + " accedió a deleteEncuestaRowData con dimensiondestino "+dimensionDestino);

        Driver driv = new Driver();

        HashMap<String, Object> map = new HashMap<String, Object>();

        String modelo = perm.modelo;
        String bd = Driver.getModelNameAsDB(modelo);

        sql = "DELETE FROM " + bd + ".dbo.asignacionTemporal" + " WHERE "
                + "_IDPeriodo = '" + periodo + "' "
                + " AND FuenteDimMemberRef2 = '" + ceco + "' "
                + " AND FuenteDimMemberRef3  COLLATE Traditional_Spanish_CI_AS in  " +
                "(SELECT distinct grupoCuenta from " + bd + ".dbo.EquivalenciaGC " +
                " WHERE _IDConductor = '" + conductor + "') "
                + " AND _NombreConductor = '" + conductor + "' "
                + " AND DimensionDestino in (" + dimensionDestino + ") ";

        Logger.info(sql);
        success = driv._queryWithoutResult(sql);

        if (success)
            map.put("success", "true");
        else
            map.put("success", "false");

        renderJSON(map);
    }

	
	/**
	 * Borra filas en encuesta
	 */
	public static void deleteEncuestaRowDataEst(int idPermiso, String fuente, String conductor, String id) {

		boolean success = false;
		String sql = null;
		HashMap<String, Object> map = new HashMap<String, Object>();

		PermisoEncuesta perm = PermisoEncuesta.findById((long) idPermiso);


		Driver driv = new Driver();

		String modelo = perm.modelo;
		String bd = Driver.getModelNameAsDB(modelo);
		String periodo = perm.periodo;
		
		sql = "DELETE FROM " + bd + ".dbo.asignacionTemporal" + " WHERE "
		 	+ "_IDPeriodo = '" + periodo + "' "
		    + " AND DimensionFuente = '" + fuente + "' "
		    + " AND _NombreConductor = '" + conductor + "' "
		    + " AND DimensionDestino = '" + id + "' ";
		    
		Logger.info(sql);
		success = driv._queryWithoutResult(sql);

		if (success)
			map.put("success", "true");
		else
			map.put("success", "false");

		renderJSON(map);
	}

	/** Modificada 
	 * Borra filas en encuesta Estadistico EP
	 */
	public static void deleteEncuestaRowDataEsts(int idPermiso, String fuente, String conductor, String id) {

		boolean success = false;
		String sql = null;
		HashMap<String, Object> map = new HashMap<String, Object>();
		String dimensionDestino = "";

		PermisoEncuesta perm = PermisoEncuesta.findById((long) idPermiso);

        String data[] = params.get("id").split(",");
        
        int i;

        for (i = 0; i < data.length; i++) {

            String datax[] = data[i].split(idSeparator);
            if (dimensionDestino.equals("")) {
                dimensionDestino = "'"+datax[0]+"'";
            } else {
                dimensionDestino = dimensionDestino +",'"+datax[0]+"'";
            }
            }

		Driver driv = new Driver();

		String modelo = perm.modelo;
		String bd = Driver.getModelNameAsDB(modelo);
		String periodo = perm.periodo;
		
		sql = "DELETE FROM " + bd + ".dbo.asignacionTemporal" + " WHERE "
		 	+ "_IDPeriodo = '" + periodo + "' "
		    + " AND DimensionFuente = '" + fuente + "' "
		    + " AND _NombreConductor = '" + conductor + "' "
//		    + " AND DimensionDestino = '" + id + "' ";
		    + " AND DimensionDestino in (" + dimensionDestino + ") ";
		    
		Logger.info(sql);
		success = driv._queryWithoutResult(sql);

		if (success)
			map.put("success", "true");
		else
			map.put("success", "false");

		renderJSON(map);
	}
	
	
	/**
	 * Uso en CRUD
	 */
	public static void showPermisoData(String modelo) {

		Driver driver = new Driver();

		// Estadistico
		TipoEncuesta tipo = TipoEncuesta.findById((long) 3);

		Map<String, Object> map = new HashMap<String, Object>();

		ArrayList<HashMap<String, String>> result = driver.getEscenario(modelo);
		ArrayList<HashMap<String, String>> result2 = driver.getPeriodo(modelo);
		ArrayList<HashMap<String, String>> result3 = driver.getCECO(modelo);

		List<Encuesta> encuestas = Encuesta.find("modelo = ? and tipo = ?",
				modelo, tipo).fetch();

		Iterator<Encuesta> it = encuestas.iterator();
		Map<String, Object>[] arraymap = (Map<String, Object>[]) new Map[encuestas
				.size()];

		int j = 0;
		while (it.hasNext()) {
			Encuesta row = it.next();

			Map<String, Object> map1 = new HashMap<String, Object>();
			map1.put("optionValue", row.id);
			map1.put("optionDisplay", row.nombre);

			arraymap[j] = map1;
			j++;
		}

		map.put("escenario", Utilities.getArrayMap(result, "Nombre", "ID"));
		map.put("periodo", Utilities.getArrayMap(result2, "Nombre", "ID"));
		map.put("ceco", Utilities.getArrayMap(result3, "ID", "Nombre"));
		map.put("encuestas", arraymap);

		renderJSON(map);
	}

	/**
	 * Dependiendo del CECO, otiene el jefe correspondiente. Uso en CRUD de
	 * PermisoEncuesta
	 */
	public static void showUserData(String ceco) {

		User usr = User.find("jefeCECO = ? and activo = true ", ceco).first();
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("usuario", usr);
		renderJSON(map);
	}

	/**
	 * Resumen de Encuesta para Funcional
	 */
	public static void showEncuestaSummaryEstadistico(int idPermiso, String dimFuente) {

		Logger.info("El usuario " + Security.connected()
				+ " accedió a la vista de mostrar resumen de estadistico");

		int preguntaPos = 0;

		String[] keys = new String[] { "FuenteDimMemberRef1",
				"FuenteDimMemberRef2", "FuenteDimMemberRef3",
				"FuenteDimMemberRef4", "FuenteDimMemberRef5",
				"FuenteDimMemberRef6", "FuenteDimMemberRef7",
				"FuenteDimMemberRef8", "FuenteDimMemberRef9",
				"FuenteDimMemberRef10", "FuenteDimMemberRef11",
				"FuenteDimMemberRef12" };

		String[] keysDestino = new String[] { "DestinoDimMemberRef1",
				"DestinoDimMemberRef2", "DestinoDimMemberRef3",
				"DestinoDimMemberRef4", "DestinoDimMemberRef5",
				"DestinoDimMemberRef6", "DestinoDimMemberRef7",
				"DestinoDimMemberRef8", "DestinoDimMemberRef9",
				"DestinoDimMemberRef10", "DestinoDimMemberRef11",
				"DestinoDimMemberRef12" };

		ArrayList<HashMap<String, String>> items = new ArrayList<HashMap<String, String>>();
		ArrayList<HashMap<String, Object>> list = new ArrayList<HashMap<String, Object>>();
		ArrayList<String> suma = new ArrayList<String>();
		ArrayList<HashMap<String, String>> estadistico = null;
		
		PermisoEncuesta perm = PermisoEncuesta.findById((long) idPermiso);
		Encuesta encuesta = perm.encuestaValidador;
		List<EncuestaForm> encuestas = encuesta.encuestas;

		String periodo = perm.periodo;
		String ceco = perm.ceco;
		String modelo = perm.modelo;
		
		Driver driver = new Driver();

		try {
		
			List<EncuestaForm> preguntas = encuesta.encuestas;
			Iterator<EncuestaForm> ite = preguntas.iterator();

			// Lineas de estadistico a ser mostrados en el resumen
			estadistico = driver.getEstadisticoRowDataSingle(modelo, dimFuente, preguntas.get(0).nombreConductor );
			
			StringBuilder sbb = new StringBuilder();

			for (String s : keys) {

				String dimension = estadistico.get(0).get(s);

				if (!dimension.equals("")) {
					sbb.append(dimension);
					sbb.append("-");
				}
			}
			
			sbb.deleteCharAt(sbb.length() - 1);
			estadistico.get(0).put("Nombre_Actividad", sbb.toString());

			while (ite.hasNext()) {

				EncuestaForm tmp = ite.next();
				ArrayList<HashMap<String, String>> result = 
					            driver.getEstadisticoRowDataSingle(modelo, dimFuente, tmp.nombreConductor);

				suma.add(driver.getFilasSumEstadistico(modelo, periodo, dimFuente, tmp.nombreConductor));

				Iterator<HashMap<String, String>> rowList = result.iterator();

				while (rowList.hasNext()) {

					HashMap<String, String> tmpRow = rowList.next();
					StringBuilder sb = new StringBuilder();

					for (String s : keysDestino) {

						String dimension = tmpRow.get(s);

						if (!dimension.equals("")) {
							sb.append(dimension);
							sb.append("-");
						}
					}
					sb.deleteCharAt(sb.length() - 1);
					tmpRow.put("Nombre_Actividad", sb.toString());
					tmpRow.put("pregunta", String.valueOf(preguntaPos));
				}

				preguntaPos++;
				items.addAll(result);
			}

			// Unir lineas iguales
			list = Utilities.getMergedList(items, preguntas.size());
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		render(estadistico, perm, encuestas, items, list, suma, dimFuente);
	}

	/**
	 * Resumen Encuesta para funcional
	 */
	public static void viewEncuestaSummaryFuncional(int idPermiso) {

		Logger.info("El usuario " + Security.connected()
				+ " accedió a la vista de mostrar resumen de cuestionario");

		PermisoEncuesta perm = PermisoEncuesta.findById((long) idPermiso);
		Encuesta encuesta = perm.encuesta;
		List<EncuestaForm> encuestas = encuesta.encuestas;

		ArrayList<HashMap<String, String>> items = new ArrayList<HashMap<String, String>>();
		ArrayList<HashMap<String, Object>> list = new ArrayList<HashMap<String, Object>>();

		ArrayList<String> suma = new ArrayList<String>();
		
		String periodo = perm.periodo;
		String ceco = perm.ceco;
		String modelo = perm.modelo;
		
		try {
			Driver driver = new Driver();

			List<EncuestaForm> preguntas = encuesta.encuestas;
			Iterator<EncuestaForm> ite = preguntas.iterator();

			String[] keys = new String[] { "DestinoDimMemberRef1",
					"DestinoDimMemberRef2", "DestinoDimMemberRef3",
					"DestinoDimMemberRef4", "DestinoDimMemberRef5",
					"DestinoDimMemberRef6", "DestinoDimMemberRef7",
					"DestinoDimMemberRef8", "DestinoDimMemberRef9",
					"DestinoDimMemberRef10", "DestinoDimMemberRef11",
					"DestinoDimMemberRef12" };

			int preguntaPos = 0;

			while (ite.hasNext()) {

				EncuestaForm tmp = ite.next();
				
				String conductor = tmp.nombreConductor;

				ArrayList<HashMap<String, String>> result = driver.getFilasEncuesta(modelo, periodo, ceco, conductor, false);
				suma.add(driver.getFilasSum(modelo, periodo, ceco, conductor));
		
				Iterator<HashMap<String, String>> rowList = result.iterator();

				while (rowList.hasNext()) {

					HashMap<String, String> tmpRow = rowList.next();
					StringBuilder sb = new StringBuilder();

					for (String s : keys) {

						String dimension = tmpRow.get(s);

						if (!dimension.equals("")) {
							sb.append(dimension);
							sb.append("-");
						}
					}
					sb.deleteCharAt(sb.length() - 1);
					tmpRow.put("Nombre_Actividad", sb.toString());
					tmpRow.put("pregunta", String.valueOf(preguntaPos));
				}
				preguntaPos++;
				items.addAll(result);
			}
			// Unir lineas iguales
			list = Utilities.getMergedList(items, preguntas.size());
		} catch (Exception e) {
			e.printStackTrace();
		}
		render(perm, encuestas, items, list, suma);
	}

	/**
	 * Enviar Encuesta a Funcional
	 */
	public static void enviarFuncional(int idPermiso) {

		// Enviado a Funcional
		Status enviFuncional = Status.findById((long) 2);
		PermisoEncuesta perm = PermisoEncuesta.findById((long) idPermiso);

		perm.status = enviFuncional;
		perm.save();

		flash.success("El cuestionario fue enviado al usuario Funcional");
		Encuestado.encuestado();
	}

	/**
	 * Cambio de Status de Encuesta
	 */
	public static void cambiarStatusFuncional(int idPermiso) {

		String save = params.get("_save");
		String reject = params.get("_reject");
		String msj = params.get("observaciones");
		String est = params.get("_est");

		if (save != null) {

			// Certificado
			Status certificado = Status.findById((long) 3);
			PermisoEncuesta perm = PermisoEncuesta.findById((long) idPermiso);

			try {
						
			} catch (Exception e) {
				e.printStackTrace();
			}

			perm.status = certificado;
			perm.save();
			flash.success("El cuestionario fue certificado!");
			
		} else if (reject != null) {

			// Rechazado
			Status rechazado = Status.findById((long) 4);
			PermisoEncuesta perm = PermisoEncuesta.findById((long) idPermiso);

			perm.status = rechazado;

			perm.observaciones = msj;
			perm.save();
			flash.success("El cuestionario fue rechazado y enviado al usuario correspondiente");
		}

		if (est != null)
			estadisticos();
		else
			funcional();
		funcional();
	}

	
	/**
	 * Cambio de Status de Encuesta
	 */
	public static void cambiarStatusEstadistico(int idPermiso, String dimFuente) {

		String save = params.get("_save");
		Driver driver = new Driver();

		if (save != null) {

			// Certificado
			Status certificado = Status.findById((long) 3);
			PermisoEncuesta perm = PermisoEncuesta.findById((long) idPermiso);

			try {
				
				boolean result = driver.certificarEncuestaRowData(perm.modelo, dimFuente);
				
				if (!result) {
					flash.success("Hubo un error al certificar el cuestionario");
					estadisticos();
				}
			
			} catch (Exception e) {
				e.printStackTrace();
			}

			perm.status = certificado;
			perm.save();
			flash.success("El cuestionario fue certificado!");
		} 
		
		estadisticos();
		
	}
}