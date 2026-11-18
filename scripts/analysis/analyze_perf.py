# Uso:  python scripts/analysis/analyze_perf.py data/campaign-2026-11-10/perfnorm-s2s3.csv
# Requiere: pandas.
# Contadores de perf (-prof perfnorm) por operación, n = 10^6. Suma los núcleos P (cpu_core) y E (cpu_atom)
# y divide por las operaciones de cada invocación (n en S2; 100 000·(k+2) en S3).
# Ojo: perfnorm cuenta todo el proceso de la JVM (GC, JIT) y el setup; usar solo para comparar estructuras.
import sys, io, re
import pandas as pd
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8")
pd.set_option("display.width", 250)

df = pd.read_csv(sys.argv[1])
df.columns = [c.replace("Param: ", "") for c in df.columns]
df["bench"] = df.Benchmark.str.extract(r"bench\.(\w+)\.")[0]
df["metric"] = df.Benchmark.str.extract(r":(.+)$")[0].fillna("time")
df["k"] = df.k.fillna(-1).astype(int)
ROUNDS = 100_000
df["ops"] = df.apply(lambda r: r.n if r.bench == "S2DeleteMin" else ROUNDS * (r.k + 2), axis=1)

t = df[df.metric == "time"].copy()
t["ns_op"] = t.Score / t.ops
cnt = df[df.metric != "time"].copy()
cnt["core"] = cnt.metric.str.extract(r"cpu_(\w+)/")[0]
cnt["ev"] = cnt.metric.str.extract(r"/([\w-]+)/")[0]
cnt["per_op"] = cnt.Score / cnt.ops
tot = cnt.pivot_table(index=["bench", "impl", "k", "ev"], columns="core", values="per_op", aggfunc="sum").fillna(0)
tot["total"] = tot.sum(axis=1)
tot["atom_share"] = tot["atom"] / tot["total"]
W = tot["total"].unstack("ev")
W["ns_op"] = t.set_index(["bench", "impl", "k"]).ns_op
W["IPC"] = W["instructions"] / W["cycles"]
W["LLC_miss_rate"] = W["LLC-load-misses"] / W["LLC-loads"]
W["atom_cycles_share"] = tot["atom_share"].unstack("ev")["cycles"]
cols = ["ns_op", "cycles", "instructions", "IPC", "L1-dcache-loads", "LLC-loads", "LLC-load-misses",
        "LLC_miss_rate", "LLC-store-misses", "dTLB-load-misses", "branch-misses", "atom_cycles_share"]
print(W[cols].round(3).to_string())

print("\n# LLC-load-misses/op y razón frente a IBH")
for b in ["S2DeleteMin", "S3Mixed"]:
    sub = W.loc[b]["LLC-load-misses"].unstack("impl")
    print(b); print(sub.round(3).to_string())
    print((sub.div(sub["IBH"], axis=0)).round(2).to_string())
print("\n# dTLB-load-misses/op")
print(W["dTLB-load-misses"].unstack("impl").round(3).to_string())
print("\n# ciclos/op")
print(W["cycles"].unstack("impl").round(1).to_string())
print("\n# fracción de ciclos en núcleos E (atom)")
print(W["atom_cycles_share"].unstack("impl").round(2).to_string())
# error relativo de los contadores
e = cnt[cnt.ev.isin(["LLC-load-misses", "cycles"]) & (cnt.core == "core")]
print("\nerror 99.9% relativo (cpu_core): mediana", (e["Score Error (99.9%)"] / e.Score).median().round(2))
print("\n# tiempo perf vs main (ns/op)")
print(t[["bench", "impl", "k", "ns_op"]].to_string())
