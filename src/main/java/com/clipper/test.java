package com.clipper;

import com.clipper.core.Paths64;
import com.clipper.core.FillRule;

/**
 * @author jiangnanboy
 * @date 2024/12/9 9:17
 */
public class test {
    public static void main(String...ars) {
        Paths64 subj = new Paths64();
        Paths64 clip = new Paths64();
        subj.add(Clipper.MakePath(new int[] { 100, 50, 10, 79, 65, 2, 65, 98, 10, 21 }));
        clip.add(Clipper.MakePath(new int[] { 98, 63, 4, 68, 77, 8, 52, 100, 19, 12 }));
        Paths64 solution = Clipper.Union(subj, clip, FillRule.NonZero);
        solution.get(0).forEach(p -> System.out.println(p.toString()));
    }
}
