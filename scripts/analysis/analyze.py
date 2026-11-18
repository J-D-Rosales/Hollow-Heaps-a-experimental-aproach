# Uso:  python scripts/analysis/analyze.py data/campaign-2026-11-10
# Requiere: pandas, numpy.
# Lee run.log (iteraciones por fork) y los CSV de la campaña, y genera las tablas del paper:
# S1/S2 y S3 en ns/op, razones de medianas con IC bootstrap jerárquico al 95 % (forks -> iteraciones,
# 2000 réplicas), sin/con CompressedOops, percentiles de S4, JOL, AllocProbe y -prof gc.
"""Análisis de la campaña full: medianas con IC bootstrap jerárquico (fork -> iteración)."""
import re, sys, io
from pathlib import Path
import numpy as np
import pandas as pd

sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8")
D = Path(sys.argv[1])
ROUNDS = 100_000
rng = np.random.default_rng(1)
B = 2000

# ---------- parseo de run.log: valores por (parte, benchmark, params, fork) ----------
rows = []
part = bench = params = None
fork = None
for line in open(D / "run.log", encoding="utf-8", errors="replace"):
    m = re.match(r"=== \[[\d:]+\] (\S+) ===", line)
    if m: part = m.group(1); continue
    m = re.match(r"# Benchmark: \S+\.(\w+)\.\w+$", line.strip())
    if m: bench = m.group(1); continue
    m = re.match(r"# Parameters: \((.*)\)", line.strip())
    if m:
        params = dict(kv.split(" = ") for kv in m.group(1).split(", ")); continue
    m = re.match(r"# Fork: (\d+) of", line.strip())
    if m: fork = int(m.group(1)); continue
    m = re.match(r"Iteration\s+\d+:\s+([\d.]+) ns/op", line.strip())
    if m and bench and bench != "S4DecreaseKey":
        rows.append(dict(part=part, bench=bench, impl=params["impl"], n=int(params["n"]),
                         k=int(params.get("k", -1)), fork=fork, v=float(m.group(1))))
raw = pd.DataFrame(rows)

def per_op(df):
    df = df.copy()
    s3 = df.bench == "S3Mixed"
    df.loc[~s3, "v"] = df.loc[~s3, "v"] / df.loc[~s3, "n"]
    df.loc[s3, "v"] = df.loc[s3, "v"] / (ROUNDS * (df.loc[s3, "k"] + 2))
    return df
raw = per_op(raw)

def boot_medians(vals_by_fork):
    """Bootstrap jerárquico: remuestrea forks y luego iteraciones dentro de cada fork."""
    forks = list(vals_by_fork)
    out = np.empty(B)
    for b in range(B):
        pick = rng.integers(0, len(forks), len(forks))
        sample = np.concatenate([rng.choice(forks[i], len(forks[i])) for i in pick])
        out[b] = np.median(sample)
    return out

cache = {}
def boots(part, bench, impl, n, k):
    key = (part, bench, impl, n, k)
    if key not in cache:
        g = raw[(raw.part == part) & (raw.bench == bench) & (raw.impl == impl) & (raw.n == n) & (raw.k == k)]
        vf = [grp.v.values for _, grp in g.groupby("fork")]
        cache[key] = (np.median(g.v.values), boot_medians(vf), len(vf), len(g))
    return cache[key]

def fmt_ci(b): lo, hi = np.percentile(b, [2.5, 97.5]); return f"[{lo:.1f}, {hi:.1f}]"

def ratio(part, bench, a, bb, n, k):
    ma, ba, *_ = boots(part, bench, a, n, k)
    mb, bbv, *_ = boots(part, bench, bb, n, k)
    r = ba / bbv
    lo, hi = np.percentile(r, [2.5, 97.5])
    rel = (ma / mb >= 1.10 or ma / mb <= 0.90) and not (lo <= 1 <= hi)
    return ma / mb, lo, hi, rel

IMPLS = ["HH", "FH", "IBH", "PQL"]
print("forks/iteraciones por config:", raw.groupby(["part","bench","impl","n","k"]).fork.nunique().unique(),
      raw.groupby(["part","bench","impl","n","k"]).size().unique())

# ---------- RQ1: S1 y S2 ----------
for bench, name in [("S1Insert", "S1 insert"), ("S2DeleteMin", "S2 deleteMin")]:
    print(f"\n## {name} (ns/op, mediana [IC95])")
    for n in (10_000, 1_000_000):
        cells = []
        for i in IMPLS:
            m, b, *_ = boots("main-s1s2s3", bench, i, n, -1)
            cells.append(f"{i} {m:.1f} {fmt_ci(b)}")
        print(f"n={n:>7}: " + " | ".join(cells))

# ---------- S3 ----------
print("\n## S3 ns/op (mediana) por k")
s3 = {}
for n in (10_000, 1_000_000):
    print(f"n={n}")
    print("   k " + "".join(f"{i:>10}" for i in IMPLS))
    for k in (0, 1, 4, 16, 64):
        vals = [boots("main-s1s2s3", "S3Mixed", i, n, k)[0] for i in IMPLS]
        s3[(n, k)] = dict(zip(IMPLS, vals))
        print(f"  {k:>2} " + "".join(f"{v:>10.1f}" for v in vals))

print("\n## Razones S3 (A/B; <1 = A más rápida). * = diferencia relevante (>10% e IC excluye 1)")
for a, bb in [("HH", "IBH"), ("HH", "FH"), ("PQL", "IBH"), ("FH", "IBH")]:
    for n in (10_000, 1_000_000):
        cells = []
        for k in (0, 1, 4, 16, 64):
            r, lo, hi, rel = ratio("main-s1s2s3", "S3Mixed", a, bb, n, k)
            cells.append(f"k={k}: {r:.2f} [{lo:.2f},{hi:.2f}]{'*' if rel else ''}")
        print(f"{a}/{bb} n={n}: " + "  ".join(cells))

# ---------- H4b: CompressedOops off/on ----------
print("\n## H4b: S3 sin/con CompressedOops (razón off/on por estructura)")
for n in (10_000, 1_000_000):
    for i in IMPLS:
        cells = []
        for k in (0, 1, 4, 16, 64):
            m_on, b_on, *_ = boots("main-s1s2s3", "S3Mixed", i, n, k)
            m_off, b_off, *_ = boots("nocoops-s3", "S3Mixed", i, n, k)
            r = b_off / b_on; lo, hi = np.percentile(r, [2.5, 97.5])
            cells.append(f"{m_off/m_on:.2f} [{lo:.2f},{hi:.2f}]")
        print(f"n={n:>7} {i:>3}: " + "  ".join(cells))

# ---------- S4 (H1) ----------
s4 = pd.read_csv(D / "main-s4.csv")
s4.columns = [c.replace("Param: ", "") for c in s4.columns]
s4["stat"] = s4.Benchmark.str.extract(r":(p[\d.]+)$")[0].fillna("mean")
piv = s4.pivot_table(index=["impl", "n"], columns="stat", values="Score")
piv["p99/p50"] = piv["p0.99"] / piv["p0.50"]
piv["p99.9/p50"] = piv["p0.999"] / piv["p0.50"]
print("\n## S4 decreaseKey aislado (ns)")
print(piv[["mean", "p0.50", "p0.90", "p0.99", "p0.999", "p0.9999", "p1.00", "p99/p50", "p99.9/p50"]].round(1).to_string())

# ---------- memoria ----------
jol = pd.read_csv(D / "jol-coops.csv"); jno = pd.read_csv(D / "jol-nocoops.csv")
print("\n## JOL (CompressedOops on): bytes de objetos por elemento / total por elemento / nodos")
for n in (10_000, 1_000_000):
    t = jol[jol.n == n].pivot_table(index=["scenario", "k"], columns="impl",
                                     values="objectBytesPerElement")[IMPLS]
    print(f"n={n}\n{t.round(1).to_string()}")
t = jol[(jol.n == 1_000_000)].pivot_table(index=["scenario", "k"], columns="impl", values="bytesPerElement")[IMPLS]
print("total B/elem n=1e6\n" + t.round(1).to_string())
nodes = jol[jol.impl.isin(["HH", "PQL"])].pivot_table(index=["n", "scenario", "k"], columns="impl", values="nodeCount")
nodes["HH/n"] = nodes["HH"] / nodes.index.get_level_values("n")
print("nodos vivos\n" + nodes.round(2).to_string())
both = jol.merge(jno, on=["scenario", "impl", "n", "k"], suffixes=("_on", "_off"))
both["off/on"] = both.totalBytes_off / both.totalBytes_on
print("JOL off/on (total)\n" + both[both.scenario == "S1"].pivot_table(index="n", columns="impl", values="off/on")[IMPLS].round(2).to_string())
print("S1 bytes por nodo on/off:", jol[(jol.scenario=="S1")&(jol.n==10000)].set_index("impl").objectBytesPerElement.to_dict(),
      jno[(jno.scenario=="S1")&(jno.n==10000)].set_index("impl").objectBytesPerElement.to_dict())

al = pd.read_csv(D / "alloc.csv")
print("\n## AllocProbe B/op (sin setup)")
print(al.pivot_table(index=["n", "k"], columns="impl", values="bytesPerOpMedian")[IMPLS].round(2).to_string())

gc = pd.read_csv(D / "gc-s3.csv"); gc.columns = [c.replace("Param: ", "") for c in gc.columns]
g = gc[gc.Benchmark.str.endswith("gc.count") | gc.Benchmark.str.endswith("gc.time")].copy()
g["m"] = g.Benchmark.str.extract(r"(gc\.\w+)$")[0]
print("\n## -prof gc: gc.count y gc.time (ms) por iteración, n=1e6")
print(g[g.n == 1_000_000].pivot_table(index=["k"], columns=["m", "impl"], values="Score").round(1).to_string())

# variabilidad
cv = raw[raw.part == "main-s1s2s3"].groupby(["bench", "impl", "n", "k"]).v.agg(lambda x: x.std() / x.mean())
print("\nCV entre iteraciones (main): mediana %.3f, máx %.3f en %s" % (cv.median(), cv.max(), cv.idxmax()))
