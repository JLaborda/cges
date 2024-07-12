library(exreport)
setwd("/Users/jdls/developer/projects/cges/notebooks/broadcasting_experiments/exreport")

# read csv results-pred-acc.csv
csv <- read.csv("exreport.csv")

# print info of variables
str(csv)

experiment <- expCreate(csv, name="cges", methods="config", problems="netName", parameters=c("threads"))
summary(experiment)

experiment <- expInstantiate(experiment, removeUnary = T)

# BDeu

testAccuracy <- testMultipleControl(experiment, "bdeu", "max")
summary(testAccuracy)

table1 <- tabularTestSummary(testAccuracy, columns =  c("pvalue", "rank", "wtl"))
table1

plot3 <- plotRankDistribution(testAccuracy)
plot3

# print report
report <- exreport("Your report")

# Add the experiment object for reference:
report <- exreportAdd(report, experiment)
# Now add the test:
report <- exreportAdd(report, testAccuracy)
# Finally you can add the different tables and plots.
report <- exreportAdd(report, list(table1,plot3))

# We create the table:
table2 <- tabularExpSummary(experiment, "bdeu", digits=4, format="f", boldfaceColumns="max", tableSplit=2)
# And add it to the report:
report <- exreportAdd(report, table2)



# SHD

testAccuracy <- testMultipleControl(experiment, "shd", "min")
summary(testAccuracy)

table1 <- tabularTestSummary(testAccuracy, columns =  c("pvalue", "rank", "wtl"))
table1

plot3 <- plotRankDistribution(testAccuracy)
plot3

# Now add the test:
report <- exreportAdd(report, testAccuracy)
# Finally you can add the different tables and plots.
report <- exreportAdd(report, list(table1,plot3))

# We create the table:
table2 <- tabularExpSummary(experiment, "shd", digits=4, format="f", boldfaceColumns="min", tableSplit=2)
# And add it to the report:
report <- exreportAdd(report, table2)


# deltaSHD
# testAccuracy <- testMultipleControl(experiment, "deltaSHD", "max")
# summary(testAccuracy)

# table1 <- tabularTestSummary(testAccuracy, columns =  c("pvalue", "rank", "wtl"))
# table1

# plot3 <- plotRankDistribution(testAccuracy)
# plot3

# # Now add the test:
# report <- exreportAdd(report, testAccuracy)
# # Finally you can add the different tables and plots.
# report <- exreportAdd(report, list(table1,plot3))

# # We create the table:
# table2 <- tabularExpSummary(experiment, "deltaSHD", digits=4, format="f", boldfaceColumns="max", tableSplit=2)
# # And add it to the report:
# report <- exreportAdd(report, table2)



# deltaSHD/emptySHD

# testAccuracy <- testMultipleControl(experiment, "deltaSHDemptySHD", "max")
# summary(testAccuracy)

# table1 <- tabularTestSummary(testAccuracy, columns =  c("pvalue", "rank", "wtl"))
# table1

# plot3 <- plotRankDistribution(testAccuracy)
# plot3

# # Now add the test:
# report <- exreportAdd(report, testAccuracy)
# # Finally you can add the different tables and plots.
# report <- exreportAdd(report, list(table1,plot3))

# # We create the table:
# table2 <- tabularExpSummary(experiment, "deltaSHDemptySHD", digits=4, format="f", boldfaceColumns="max", tableSplit=2)
# # And add it to the report:
# report <- exreportAdd(report, table2)



# deltaBDeu

# testAccuracy <- testMultipleControl(experiment, "deltaBDeu", "max")
# summary(testAccuracy)

# table1 <- tabularTestSummary(testAccuracy, columns =  c("pvalue", "rank", "wtl"))
# table1

# plot3 <- plotRankDistribution(testAccuracy)
# plot3

# # Now add the test:
# report <- exreportAdd(report, testAccuracy)
# # Finally you can add the different tables and plots.
# report <- exreportAdd(report, list(table1,plot3))

# # We create the table:
# table2 <- tabularExpSummary(experiment, "deltaBDeu", digits=4, format="f", boldfaceColumns="max", tableSplit=2)
# # And add it to the report:
# report <- exreportAdd(report, table2)


# delta/s	

# testAccuracy <- testMultipleControl(experiment, "deltas", "max")
# summary(testAccuracy)

# table1 <- tabularTestSummary(testAccuracy, columns =  c("pvalue", "rank", "wtl"))
# table1

# plot3 <- plotRankDistribution(testAccuracy)
# plot3

# # Now add the test:
# report <- exreportAdd(report, testAccuracy)
# # Finally you can add the different tables and plots.
# report <- exreportAdd(report, list(table1,plot3))

# # We create the table:
# table2 <- tabularExpSummary(experiment, "deltas", digits=4, format="f", boldfaceColumns="max", tableSplit=2)
# # And add it to the report:
# report <- exreportAdd(report, table2)


# Render the report:
exreportRender(report, target = "HTML", destination=".", visualize = T)
