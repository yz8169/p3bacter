import numpy as np
import pandas as pd
import plotly
import plotly.graph_objs as go
import argparse

parser = argparse.ArgumentParser()
parser.add_argument("--pCutoff", type=float, default=0.05, help="P value cut off")
parser.add_argument("--qCutoff", type=float, default=1, help="fdr value cut off")
parser.add_argument("--l", type=float, default=1, help="logFc value cut off")
args = parser.parse_args()
df = pd.read_csv("allResult.txt", sep='\t')
df.columns = ["geneId", "mean1", "std1", "mean2", "std2", "logFc", "p", "fdr"]
someDf = df[(df['p'] < args.pCutoff) & (df['fdr'] < args.qCutoff) & (abs(df['logFc']) > args.l)]
noneDf = df[(df['p'] >= args.pCutoff) | (df['fdr'] >= args.qCutoff) | (abs(df['logFc']) <= args.l)]
none = go.Scatter(
    x=noneDf["logFc"],
    y=-np.log10(noneDf["p"]),
    mode='markers',
    marker=dict(
        color='black'
    ),
    text=noneDf["geneId"].apply(lambda x: "Protein:" + x),
    hoverinfo='x+y+text',
    name="None:"+str(len(noneDf))
)
upDf = someDf[someDf['logFc'] > 0]
downDf = someDf[someDf['logFc'] <= 0]
up = go.Scatter(
    x=upDf.ix[:, 5],
    y=-np.log10(upDf["p"]),
    mode='markers',
    marker=dict(
        color='red'
    ),
    text=upDf["geneId"].apply(lambda x: "Protein:" + x),
    hoverinfo='x+y+text',
    name="Up:"+str(len(upDf))
)
down = go.Scatter(
    x=downDf.ix[:, 5],
    y=-np.log10(downDf["p"]),
    mode='markers',
    marker=dict(
        color='blue'
    ),
    text=downDf["geneId"].apply(lambda x: "Protein:" + x),
    hoverinfo='x+y+text',
    name="Down:"+str(len(downDf))
)
data = [none, up, down]
layout = go.Layout(
    title="Volcano Plot",
    xaxis=dict(
        title='logFC',
        fixedrange=True,
        showgrid=False
    ),
    yaxis=dict(
        title='-log10(p)',
        fixedrange=True,
        showgrid=False
    ),
    hovermode="closest",
    height=600,
    width=800
)
config = dict(
    displaylogo=False,
    showLink=False,
    # modeBarButtonsToRemove=["plotGlPixelRatio"],
)
fig = dict(data=data, layout=layout)
# plotly.offline.plot(fig, filename="basic-scatter.html", config=config)
a = plotly.offline.plot(fig, filename="basic-scatter.html", config=config, output_type="div",include_plotlyjs=False)
f = open('div.txt', 'w')
f.write(a)
f.close()
