package mc3d;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Charge un fichier .obj (positions + coordonnees de texture) en un Mesh.
 *
 * Astuce "aplatir" : au lieu de reconstruire des index facon OpenGL (penible car
 * l'OBJ indexe positions et UV separement), on recopie chaque sommet de chaque face
 * directement dans les tableaux de sortie, puis on genere des index triviaux 0,1,2,...
 * => simple, et le Mesh existant est reutilise tel quel.
 */
public class ObjLoader {

	public static Mesh load(String chemin) {
		List<float[]> vPositions = new ArrayList<>(); // chaque "v"  = {x, y, z}
		List<float[]> vtTexcoords = new ArrayList<>(); // chaque "vt" = {u, v}

		List<Float> outPositions = new ArrayList<>();
		List<Float> outUvs = new ArrayList<>();

		List<String> lignes;
		try {
			lignes = Files.readAllLines(Paths.get(chemin));
		} catch (Exception e) {
			throw new RuntimeException("Impossible de lire l'OBJ : " + chemin, e);
		}

		for (String ligne : lignes) {
			ligne = ligne.trim();
			if (ligne.isEmpty() || ligne.startsWith("#")) continue;
			String[] t = ligne.split("\s+");

			switch (t[0]) {
				case "v" -> vPositions.add(new float[] {
						Float.parseFloat(t[1]), Float.parseFloat(t[2]), Float.parseFloat(t[3]) });
				case "vt" -> vtTexcoords.add(new float[] {
						Float.parseFloat(t[1]), Float.parseFloat(t[2]) });
				case "f" -> {
					// Triangulation en eventail : gere triangles ET quads/n-gons
					for (int i = 2; i + 1 < t.length; i++) {
						ajouterSommet(t[1],     vPositions, vtTexcoords, outPositions, outUvs);
						ajouterSommet(t[i],     vPositions, vtTexcoords, outPositions, outUvs);
						ajouterSommet(t[i + 1], vPositions, vtTexcoords, outPositions, outUvs);
					}
				}
				default -> { /* vn, o, s, usemtl, mtllib... : ignores */ }
			}
		}

		float[] positions = toArray(outPositions);
		float[] uvs = toArray(outUvs);
		int[] indices = new int[positions.length / 3];
		for (int i = 0; i < indices.length; i++) indices[i] = i;

		return new Mesh(positions, uvs, indices);
	}

	/** Lit un sommet de face "pos/tex/normale" et recopie position + UV dans les sorties. */
	private static void ajouterSommet(String token, List<float[]> vPos, List<float[]> vtTex,
			List<Float> outPos, List<Float> outUv) {
		String[] p = token.split("/");
		int vi = Integer.parseInt(p[0]) - 1; // ATTENTION : l'OBJ est indexe a partir de 1
		float[] position = vPos.get(vi);
		outPos.add(position[0]);
		outPos.add(position[1]);
		outPos.add(position[2]);

		if (p.length >= 2 && !p[1].isEmpty()) {
			int ti = Integer.parseInt(p[1]) - 1;
			float[] tex = vtTex.get(ti);
			outUv.add(tex[0]);
			outUv.add(tex[1]);
		} else {
			outUv.add(0f); // pas d'UV dans le fichier -> 0,0
			outUv.add(0f);
		}
	}

	private static float[] toArray(List<Float> list) {
		float[] a = new float[list.size()];
		for (int i = 0; i < a.length; i++) a[i] = list.get(i);
		return a;
	}
}
