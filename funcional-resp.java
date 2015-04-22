package controllers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import models.Encuesta;
import models.EncuestaForm;
import models.PermisoEncuesta;
import models.Status;
import models.TipoEncuesta;
import models.User;
import play.Logger;
import play.mvc.Controller;
import play.mvc.With;
import utils.Utilities;

import com.google.gson.Gson;

import db.Driver;
import db.UUIDGenerator;

/**
 * Controlador modulo Funcional de SASWeb
 * 
 * @author Gerardo Curiel <gcuriel@0269.com.ve>
 * 
 */
@With(Secure.class)
public class Funcional extends Controller {

	/**
	 * Vista de Ordenes
	 */
	
	static String idSeparator = ";";
	
	@Check("funcional")
	public static void ordenes() {

		Logger.info("El usuario " + Security.connected()
				+ " accedió a la vista Ordenes");

		String usr = Security.connected();
		User user = User.find("byUsuario", usr).<User> first();

		Status noEnv = Status.findById((long) 1);
		// Enviado a funcional
		Status envi = Status.findById((long) 2);
		Status validado = Status.findById((long) 3);
		Status rechazado = Status.findById((long) 4);

		TipoEncuesta tipo = TipoEncuesta.findById((long) 1);

		List<PermisoEncuesta> noactualizados = PermisoEncuesta
				.find("(status = ? or status = ?) and usuario = ? and __Activo = true and encuesta.tipo = ? ",
						noEnv, rechazado, user, tipo).fetch();
		
		List<PermisoEncuesta> actualizados = PermisoEncuesta
				.find("(status = ? or status = ?) and usuario = ? and __Activo = true and encuesta.tipo = ? ",
						envi, validado, user, tipo).fetch();
		render(noactualizados, actualizados);
	}

	/**
	 * Uso en CRUD de PermisoEncuesta
	 */
	@Check("funcional")
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
	 * Llenado de Ordenes
	 */
	@Check("funcional")
	public static void llenarOrden(int idPermiso) {

		Logger.info("El usuario " + Security.connected()
				+ " accedió a la vista de llenar Encuesta");

		//
		ArrayList<HashMap<String, String>> actividades;
		HashMap<String, List> selectBoxes;

		PermisoEncuesta perm = PermisoEncuesta.findById((long) idPermiso);
		Encuesta encuesta = perm.encuesta;

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

		render(perm, encuesta, actividades, selectBoxes);
	}

	/**
	 * Borrar fila de encuesta
	 */
	@Check("funcional")
	public static void deleteEncuestaRowData(int idPermiso, int idPregunta) {

		boolean success = false;
		String sql = null;

		String idInsert = params.get("id");
        //String idInsert = params.get("idPermiso");

		PermisoEncuesta perm = PermisoEncuesta.findById((long) idPermiso);

		Driver driv = new Driver();

		HashMap<String, Object> map = new HashMap<String, Object>();

		String modelo = Driver.getModelNameAsDB(perm.modelo);

		sql = "DELETE FROM " + modelo + ".dbo.asignacionTemporal" + " WHERE "
				+ "_idInsert = '" + idInsert + "' ;";

		System.out.println(sql);
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

    /** req 26591 EP 10.03.2015
     * Descripcion: Funcion que elimina una encuesta
     * Borrar Encuesta
     * Parametros:
     * idPermiso: identificador unico de encuesta en permisoencuesta
     * idopcion: identificador que define redireccion a revision encuesta 1 o estadistico 0
     */
    @Check("funcional")
    public static void deleteEncuesta(int idPermiso, int idopcion) {

        boolean success = false;
        String sql = null;


        String id = params.get("idPermiso");
        String idop = params.get("idopcion");

        //PermisoEncuesta perm = PermisoEncuesta.findById((long) idPermiso);

        Driver driv = new Driver();

        HashMap<String, Object> map = new HashMap<String, Object>();

        //String modelo = Driver.getModelNameAsDB(perm.modelo);

        sql = "DELETE FROM  SASweb.dbo.PermisoEncuesta" + " WHERE "
                + "id = '" + id + "' ;";

        System.out.println(sql);
        success = driv._queryWithoutResult(sql);

        if (success) {
            //map.put("success", "true");
            if (idop.equals("0")) {
                flash.success("El estadistico fue Eliminado!");
                Application.estadisticos();
            } else {
                flash.success("La encuesta fue Eliminada!");
                Encuestado.encuestado();
            }

        } else {
            map.put("success", "false");
            map.put("message",
                    "Error al Borrar");
        }
        renderJSON(map);
    }


    /**
	 * Insertar fila en encuesta
	 */
	public static void insertEncuestaRowData(int idPermiso, int idPregunta) {

		Logger.debug("insertEncuestaRowData");
		PermisoEncuesta perm = PermisoEncuesta.findById((long) idPermiso);

		// Obtener parametros
		String activo = "0";
		String bd = Driver.getModelNameAsDB(perm.modelo);
		String modelo = perm.modelo;

		String periodo = perm.periodo;
		String escenario = perm.escenario;

		StringBuffer dimValues = new StringBuffer();
		String DQF;

		EncuestaForm encuestaPreg = EncuestaForm.findById((long) idPregunta);
		String conductor = encuestaPreg.nombreConductor;

		Driver driv = new Driver();

		// Dimension Fuente
		String tipoModuloABCFuente = "RESOURCE";
		ArrayList<HashMap<String, String>> filales = driv.getFilial();

		String ceco = perm.ceco;

		ArrayList<HashMap<String, String>> ctas = driv.getCuenta(conductor,
				modelo);

		// Si no existe conductor asociado
		HashMap<String, Object> map = new HashMap<String, Object>();
		if (ctas.size() == 0) {
			map.put("success", "false");
			map.put("message",
					"No existe conductor asociado. Por favor seleccionar otras combinación");
			renderJSON(map);
		}

		String cta = ctas.get(0).get("GrupoCuenta");

		// Cambiar choices
		// JSON parsing
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

		boolean success = false;

		String idInsert = UUIDGenerator.getUUID();

		// Si el tipoValor es un porcentaje
		if (encuestaPreg.tipoValor.nombre.equals("Porcentaje")) {
			// Obtener total de DQF de una pregunta
			HashMap<String, String> dqfSum = driv.getDQFSum(bd, idPermiso, idPregunta);

			if (!dqfSum.get("Total").equals("")) {

				float dqfTotal = Float.parseFloat(dqfSum.get("Total"));
				int totalAgregado = (int) (dqfTotal / filales.size())
						+ Integer.parseInt(DQF);
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

			filial = filales.get(i).get("Nombre");
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
					+ idPermiso + "', '" + idPregunta + "', '"
					+ Utilities.getTimeStamp() + "' , '" + idInsert + "') ;";
			Logger.info(sql);
			success = driv._queryWithoutResult(sql);

			if (!success)
				break;
		}

		if (success) {
			map.put("success", "true");
		} else {
			map.put("success", "false");
		}
		renderJSON(map);
	}

	/**
	 * Cambiar status de Orden
	 */
	@Check("funcional")
	public static void cambiarStatusOrden(int idPermiso) {

		// Enviado a Funcional
		Status enviFuncional = Status.findById((long) 2);
		PermisoEncuesta perm = PermisoEncuesta.findById((long) idPermiso);

		perm.status = enviFuncional;
		perm.save();

		flash.success("La Orden fue certificada!");
		ordenes();
	}

	/**
	 * Resumen de Orden
	 */
	@Check("funcional")
	public static void showOrdenSummary(int idPermiso, String ceco, String periodo) {

		Logger.info("El usuario " + Security.connected()
				+ " accedió a la vista de mostrar resumen de orden");

		PermisoEncuesta perm = PermisoEncuesta.findById((long) idPermiso);
		Encuesta encuesta = perm.encuesta;
		List<EncuestaForm> encuestas = encuesta.encuestas;

		ArrayList<HashMap<String, String>> items = new ArrayList<HashMap<String, String>>();
		ArrayList<HashMap<String, Object>> list = new ArrayList<HashMap<String, Object>>();

		ArrayList<String> suma = new ArrayList<String>();
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

				ArrayList<HashMap<String, String>> result = 
	                                   driver.getFilasEncuesta(modelo, periodo, ceco, conductor, false);

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

}
