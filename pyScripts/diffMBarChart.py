import pandas as pd
import argparse
import plotly.graph_objs as go
import plotly
from plotly import tools

parser = argparse.ArgumentParser()
args = parser.parse_args()

df = pd.read_csv("deal.txt", sep='\t', index_col=0)
geneDf = pd.read_csv("gene.txt", sep='\t',header=None)
g = geneDf[0][0]
groupDf = pd.read_csv("group.txt", sep='\t')
columnNames = df.columns
barData = []
groups = groupDf.groupby("Group")
i = 0
for name, group in groups:
    eachGroupDf = df[group["Sample"]]
    color = "#1F77B4"
    if (i == 0):
        color = "#1F77B4"
    else:
        color = "#FF7F0E"
    i = i + 1
    bar = go.Bar(
        x=eachGroupDf.columns,
        y=eachGroupDf.ix[g],
        hoverinfo='x+y',
        name=name,
        showlegend=True,
        marker=dict(
            color=color
        ),

    )
    barData.append(bar)
data = []
i = 0
for name, group in groups:
    eachGroupDf = df[group["Sample"]]
    if (i == 0):
        color = "#1F77B4"
    else:
        color = "#FF7F0E"
    i = i + 1
    box = go.Box(
        y=eachGroupDf.ix[g],
        hoverinfo='y',
        name=name,
        showlegend=False,
        marker=dict(
            color=color,
        ),
        # fillcolor="white"
    )
    data.append(box)
config = dict(
    displaylogo=False,
    showLink=False,
)

fig = tools.make_subplots(rows=2, cols=1,
                          subplot_titles=("Bar Plot", "Box Plot"))
fig.append_trace(barData[0], 1, 1)
fig.append_trace(barData[1], 1, 1)
fig.append_trace(data[0], 2, 1)
fig.append_trace(data[1], 2, 1)
fig["layout"].update(
    width="800",
    height="600",
    hovermode="closest",

)
fig["layout"]["yaxis1"].update(
    title="Expression",
    showgrid=False,
    zeroline=False,
    ticks='outside',
    showline=True,
)
fig["layout"]["xaxis1"].update(
    title="Metabolite:" + g,
    showgrid=False,
    zeroline=False,
    ticks='outside',
    showline=True,
)
fig["layout"]["yaxis2"].update(
    title="Expression",
    showgrid=False,
    zeroline=False,
    ticks='outside',
    showline=True,
)
fig["layout"]["xaxis2"].update(
    title="Metabolite:" + g,
    showgrid=False,
    zeroline=False,
    ticks='outside',
    showline=True,
)
# plotly.offline.plot(fig, filename="basic-scatter.html", config=config)
a = plotly.offline.plot(fig, filename="basic-scatter.html", config=config, output_type="div", include_plotlyjs=False)
f = open('div.txt', 'w')
f.write(a)
f.close()
